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

require.config({
    baseUri: '.',
    paths: {
        "text" : "lib/text",
        "jquery" : "lib/jquery-1.10.2",
        "underscore" : "lib/underscore",
        "bootstrap" : "lib/bootstrap",
        "datatable" : "lib/jquery.dataTables",
        "d3" : "lib/d3.v3",
        "nvd3" : "lib/nv.d3",
        "ace" : "lib/ace-min/ace",
        "jquery.filedrop" : "lib/jquery.filedrop"
    },

    shim: {
        'jquery' : {
            exports: '$'
        },

        'underscore' : {
            deps : ['jquery'],
            exports: '_'
        },

        'bootstrap' : {
            deps : ['jquery']
        },

        'd3' : {
            deps : ['jquery']
        },

        'nvd3' : {
            deps : ['d3']
        },

        'table' : {
            deps : ['jquery', 'bootstrap', 'datatable'],
            exports: 'Table'
        },

        'ace' : {
            deps : ['jquery'],
            exports: 'ace'
        }
    }
});

// render the first page
require([
    'router',
    'jquery',
    'text',
    'underscore',
    'bootstrap',
    'd3',
    'nvd3',
    'table'
], function(
    Router,
    JQuery,
    Text,
    Underscore,
    Bootstrap,
    D3,
    Nvd3,
    Table
) {
    Router.start();
    Table.init();
    Router.redirect('#project');
});
