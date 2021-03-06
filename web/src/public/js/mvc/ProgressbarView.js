/*
 * QCRI, NADEEF LICENSE
 * NADEEF is an extensible, generalized and easy-to-deploy data cleaning platform built at QCRI.
 * NADEEF means "Clean" in Arabic
 *
 * Copyright (c) 2011-2013, Qatar Foundation for Education, Science and Community Development (on
 * behalf of Qatar Computing Research Institute) having its principle place of business in Doha,
 * Qatar with the registered address P.O box 5825 Doha, Qatar (hereinafter referred to as "QCRI")
 *
 * NADEEF has patent pending nevertheless the following is granted.
 * NADEEF is released under the terms of the MIT License, (http://opensource.org/licenses/MIT).
 */

define(
	['text!mvc/template/progressbar.template.html'], 
	function(ProgressBarTemplate) {
		var isSubscribed = false;
		function start(id) {
			if (!isSubscribed) {
				setInterval(function() {
					$.getJSON('/progress', function(data) {
						var values = new Array();
						_.each(data['data'], function(v) {
							var name = 
								localStorage[v['key']] ? 
                                localStorage[v['key']] : 'Unknown';
							values.push({ 
								name : name,
								value : v['overallProgress'] 
							});
						});
					    var html = 
                            _.template(ProgressBarTemplate)({ progress: values });
						$('#' + id).html(html);
					})
                    .fail(function(request, status, err) {
                        console.log("Requesting progress failed : " + request.responseText);
                    });
				}, 1000);
				isSubscribed = true;
			}
		}

		return {
			start: start
		};
	}
);
