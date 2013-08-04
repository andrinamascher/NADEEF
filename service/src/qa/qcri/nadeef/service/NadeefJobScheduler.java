
package qa.qcri.nadeef.service;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.*;
import qa.qcri.nadeef.core.datamodel.CleanPlan;
import qa.qcri.nadeef.core.pipeline.CleanExecutor;
import qa.qcri.nadeef.service.thrift.TJobStatus;
import qa.qcri.nadeef.service.thrift.TJobStatusType;
import qa.qcri.nadeef.tools.Tracer;

import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;

/**
 * NADEEF job scheduler.
 */
public class NadeefJobScheduler {
    private static NadeefJobScheduler instance;
    private static ListeningExecutorService service;
    private static ConcurrentMap<String, NadeefJob> runningCleaner;
    private static ConcurrentMap<String, String> runningRules;
    private static String hostname;
    private static Tracer tracer = Tracer.getTracer(NadeefServiceHandler.class);

    static {
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            tracer.err("Unknown hostname", e);
            hostname = "localhost";
        }

        // TODO: move to per process approach or multithreading, currently it is
        // limited to 1 because of synchronization of DBConnection factory.
        service = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(1));
        runningCleaner = Maps.newConcurrentMap();
        runningRules = Maps.newConcurrentMap();
    } 

    private enum JobType {
        Detect,
        Repair
    }

    /**
     * NadeefJob class represents a runnable job.
     */
    private static class NadeefJob {
        NadeefJob(String key, CleanExecutor executor, JobType type) {
            this.key = key;
            this.executor = executor;
            this.type = type;
        }

        public String key;
        public CleanExecutor executor;
        public JobType type;
    }

    /**
     * Container for executing a clean plan.
     */
    class CleanExecutorCaller implements Callable<String> {
        private WeakReference<NadeefJob> jobRef;

        public CleanExecutorCaller(WeakReference<NadeefJob> jobRef) {
            Preconditions.checkNotNull(jobRef);
            this.jobRef = jobRef;
        }

        public String call() throws Exception {
            NadeefJob job = jobRef.get();
            if (job == null) {
                throw new NullPointerException("Job reference is null in execution.");
            }

            switch (job.type) {
                case Detect:
                    job.executor.detect();
                    break;
                case Repair:
                    job.executor.repair();
                    break;
            }
            return job.key;
        }
    }

    /**
     * Callback function once a clean is done.
     */
    class CleanCallback implements FutureCallback<String> {
        private Tracer tracer = Tracer.getTracer(CleanCallback.class);

        public void onSuccess(String key) {
            runningCleaner.remove(key);
            runningRules.remove(key);
        }

        @Override
        public void onFailure(Throwable throwable) {
            tracer.err("FutureCallback failed.");
        }
    }

    /**
     * Singleton access.
     * @return NadeefJobScheduler.
     */
    public synchronized static NadeefJobScheduler getInstance() {
        if (instance == null) {
            instance = new NadeefJobScheduler();
        }
        return instance;
    }

    /**
     * Submits a detection job.
     * @param cleanPlan clean plan.
     * @return job key.
     */
    public String submitDetectJob(CleanPlan cleanPlan) {
        NadeefJob job = createNewJob(cleanPlan, JobType.Detect);
        ListenableFuture<String> future =
            service.submit(new CleanExecutorCaller(new WeakReference<>(job)));
        Futures.addCallback(future, new CleanCallback());
        return job.key;
    }

    /**
     * Submits a repair job.
     * @param cleanPlan clean plan.
     * @return job key.
     */
    public String submitRepairJob(CleanPlan cleanPlan) {
        NadeefJob job = createNewJob(cleanPlan, JobType.Repair);

        ListenableFuture<String> future =
            service.submit(new CleanExecutorCaller(new WeakReference<>(job)));
        Futures.addCallback(future, new CleanCallback());
        return job.key;
    }

    /**
     * Gets the status of a given job key.
     * @param key job key.
     * @return {@link TJobStatus}.
     */
    // TODO: should we separate Thrift code?
    public TJobStatus getJobStatus(String key) {
        TJobStatus result = new TJobStatus();
        if (!runningCleaner.containsKey(key)) {
            result.setStatus(TJobStatusType.NOTAVAILABLE);
            return result;
        }

        NadeefJob job = runningCleaner.get(key);
        CleanExecutor executor = job.executor;
        double progressd = 0f;
        switch(job.type) {
            case Detect:
                progressd = executor.getDetectPercentage();
                break;
            case Repair:
                progressd = executor.getRepairPercentage();
                break;
        }
        result.setProgress((int)(progressd * 100));
        // a hack to determine whether the job is executing or not.
        if (executor.isRunning()) {
            result.setStatus(TJobStatusType.RUNNING);
        } else {
            result.setStatus(TJobStatusType.WAITING);
        }

        result.setKey(key);
        return result;
    }

    /**
     * Gets the job status of all running jobs.
     * @return the job status of all running jobs.
     */
    public List<TJobStatus> getJobStatus() {
        Set<String> keys = runningCleaner.keySet();
        List<TJobStatus> result = Lists.newArrayList();
        for (String key : keys) {
            result.add(getJobStatus(key));
        }
        return result;
    }

    private static synchronized NadeefJob createNewJob(CleanPlan cleanPlan, JobType type) {
        Preconditions.checkNotNull(cleanPlan);

        String ruleName = cleanPlan.getRule().getRuleName();
        if (runningRules.containsValue(ruleName)) {
            tracer.info("Submitting duplicate rules.");
        }

        String key;

        while (true) {
            key = hostname + "_" + UUID.randomUUID().toString();
            if (!runningCleaner.containsKey(key)) {
                break;
            }
        }
        NadeefJob job = new NadeefJob(key, new CleanExecutor(cleanPlan), type);
        runningCleaner.put(key, job);
        runningRules.put(key, ruleName);
        return job;
    }
}
