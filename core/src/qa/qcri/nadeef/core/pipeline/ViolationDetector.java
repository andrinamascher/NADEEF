/*
 * QCRI, NADEEF LICENSE
 * NADEEF is an extensible, generalized and easy-to-deploy data cleaning platform built at QCRI.
 * NADEEF means â€œCleanâ€� in Arabic
 *
 * Copyright (c) 2011-2013, Qatar Foundation for Education, Science and Community Development (on
 * behalf of Qatar Computing Research Institute) having its principle place of business in Doha,
 * Qatar with the registered address P.O box 5825 Doha, Qatar (hereinafter referred to as "QCRI")
 *
 * NADEEF has patent pending nevertheless the following is granted.
 * NADEEF is released under the terms of the MIT License, (http://opensource.org/licenses/MIT).
 */

package qa.qcri.nadeef.core.pipeline;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import qa.qcri.nadeef.core.datamodel.IteratorStream;
import qa.qcri.nadeef.core.datamodel.Rule;
import qa.qcri.nadeef.core.datamodel.Table;
import qa.qcri.nadeef.core.datamodel.Tuple;
import qa.qcri.nadeef.core.datamodel.TuplePair;
import qa.qcri.nadeef.core.datamodel.Violation;
import qa.qcri.nadeef.tools.Tracer;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * Wrapper class for executing the violation detection.
 */
public class ViolationDetector<T>
    extends Operator<Rule, Collection<Violation>> {
    private static final int MAX_THREAD_NUM = Runtime.getRuntime().availableProcessors() * 2;

    private Rule rule;
    private Collection<Violation> resultCollection;
    private ListeningExecutorService service;

    private Object lock = new Object();
    
    private int totalThreadCount;
    private int finishedThreadCount;
    private int detectCount;

    /**
     * Violation detector constructor.
     * @param rule rule.
     */
    public ViolationDetector(Rule rule) {
        Preconditions.checkNotNull(rule);
        resultCollection = Lists.newArrayList();
        ThreadFactory factory = new ThreadFactoryBuilder().setNameFormat("detect-pool-%d").build();
        service =
            MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(MAX_THREAD_NUM, factory));
    }

    /**
     * Callback class for progress report.
     */
    class DetectorCallback implements FutureCallback<Integer> {
        @Override
        public void onSuccess(Integer integer) {
            synchronized (ViolationDetector.class) {
                finishedThreadCount ++;
                detectCount += integer;
                setPercentage(finishedThreadCount / totalThreadCount);
            }
        }

        @Override
        public void onFailure(Throwable throwable) {}
    }

    /**
     * Detector callable class.
     */
    class Detector implements Callable<Integer> {
        private List<Object> tupleList;
        private Object lock;
        
        public Detector(List<Object> tupleList) {
            this.tupleList = tupleList;
            this.lock = lock;
        }

        /**
         * Computes a result, or throws an exception if unable to do so.
         *
         * @return detection count
         * @throws Exception if unable to compute a result
         */
        @Override
        @SuppressWarnings("unchecked")
        public Integer call() throws Exception {
            int count = 0;
            Collection<Violation> result = Lists.newArrayList();
            for (int i = 0; i < tupleList.size(); i ++) {
                Object item = tupleList.get(i);

                // breakage in the buffer
                if (item == null) {
                    continue;
                }

                count ++;
                Collection<Violation> violations = null;
                if (rule.supportOneInput()) {
                    Tuple tuple = (Tuple)item;
                    violations = rule.detect(tuple);
                } else if (rule.supportTwoInputs()) {
                    TuplePair pair = (TuplePair)item;
                    violations = rule.detect(pair);
                } else if (rule.supportManyInputs()) {
                    Table collection = (Table)item;
                    violations = rule.detect(collection);
                }

                if (violations != null && violations.size() > 0) {
                    result.addAll(violations);
                }
            }
            
            // TODO comment by Ian, this is potential bottleneck...
            synchronized (ViolationDetector.class) {
                resultCollection.addAll(result);
            }
            // This is to reclaim the memory back.
            tupleList = null;
            return count;
        }
    }

    /**
     * Execute the operator.
     *
     * @param rule rule.
     * @return list of violations.
     */
    @Override
    @SuppressWarnings("unchecked")
    public Collection<Violation> execute(Rule rule) throws Exception {
        this.rule = rule;
        IteratorStream iteratorStream = new IteratorStream<T>();
        resultCollection.clear();
        List<Object> tupleList;
        long elapsedTime = 0l;
        detectCount = 0;
        
        while (true) {
            tupleList = iteratorStream.poll();
            if (tupleList.size() == 0) {
                break;
            }

            totalThreadCount ++;
            ListenableFuture<Integer> future = service.submit(new Detector(tupleList));
            Futures.addCallback(future, new DetectorCallback());

            // comment by Ian: here we need a synchronized action to make sure data filled into resultCollection, otherwise there will be racing problem!
            while (!future.isDone() && !future.isCancelled()){
            	Thread.sleep(1000);
            }
        }

        Tracer.putStatsEntry(Tracer.StatType.DetectCallTime, elapsedTime);
        Tracer.putStatsEntry(Tracer.StatType.DetectCount, detectCount);
        Tracer.putStatsEntry(Tracer.StatType.DetectThreadCount, totalThreadCount);

        return resultCollection;
    }

    @Override
    public void finalize() throws Throwable {
        super.finalize();
        if (!service.isShutdown()) {
            service.shutdownNow();
        }
    }
}