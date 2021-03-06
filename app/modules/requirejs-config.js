/*global require*/
'use strict';
require.config({
  map: {
    '*': {
      'css': 'require-css/css',
      'text': 'requirejs-text/text'
    }
  },
  paths: {
    'require-css': '../bower_components/require-css',
    'requirejs-text': '../bower_components/requirejs-text',

    'togetherjs': 'https://togetherjs.com/togetherjs-min',

    'bootstrap-css': '../bower_components/bootstrap/dist/css/bootstrap',
    'bootstrap.affix': '../bower_components/bootstrap/js/affix',
    'bootstrap.alert': '../bower_components/bootstrap/js/alert',
    'bootstrap.button': '../bower_components/bootstrap/js/button',
    'bootstrap.carousel': '../bower_components/bootstrap/js/carousel',
    'bootstrap.transition': '../bower_components/bootstrap/js/transition',
    'bootstrap.collapse': '../bower_components/bootstrap/js/collapse',
    'bootstrap.dropdown': '../bower_components/bootstrap/js/dropdown',
    'bootstrap.modal': '../bower_components/bootstrap/js/modal',
    'bootstrap.scrollspy': '../bower_components/bootstrap/js/scrollspy',
    'bootstrap.tab': '../bower_components/bootstrap/js/tab',
    'bootstrap.tooltip': '../bower_components/bootstrap/js/tooltip',
    'bootstrap.popover': '../bower_components/bootstrap/js/popover',
    'bootstrap-hover-dropdown': '../bower_components/bootstrap-hover-dropdown/bootstrap-hover-dropdown',
    'bootstrap-datepicker': '../bower_components/bootstrap-datepicker/js/bootstrap-datepicker',
    'bootstrap-datepicker.el': '../bower_components/bootstrap-datepicker/js/locales/bootstrap-datepicker.el',
    'bootstrap-datepicker-css': '../bower_components/bootstrap-datepicker/css/datepicker',

    'jquery': '../bower_components/jquery/jquery',
    'jquery-backstretch': '../bower_components/jquery-backstretch/jquery.backstretch',
    'jquery-migrate': '../bower_components/jquery-migrate/jquery-migrate',
    'jquery-blockUI': '../bower_components/blockui/jquery.blockUI',
    'jquery-cookie': '../bower_components/jquery-cookie/jquery.cookie',
    'jquery-uniform': '../bower_components/jquery.uniform/jquery.uniform',
    'jquery-validate': '../bower_components/jquery-validation/jquery.validate',
    'jquery-validate.el': '../bower_components/jquery-validation/localization/messages_el',
    'select2': '../bower_components/select2/select2',

    'excel-builder': '../bower_components/excelbuilder/dist/excel-builder.compiled',
    'saveAs': '../bower_components/FileSaver/FileSaver',

    'angular-file-upload-html5-shim': '../bower_components/ng-file-upload/angular-file-upload-html5-shim',
    'angular-file-upload': '../bower_components/ng-file-upload/angular-file-upload',
    'angular': '../bower_components/angular/angular',
    'angular-locale_el': '../bower_components/angular-i18n/angular-locale_el',
    'angular-route': '../bower_components/angular-route/angular-route',
    'angular-mocks': '../bower_components/angular-mocks/angular-mocks',
    'angular-resource': '../bower_components/angular-resource/angular-resource',
    'angular-cookies': '../bower_components/angular-cookies/angular-cookies',
    'angular-sanitize': '../bower_components/angular-sanitize/angular-sanitize',
    'angular-base64': '../bower_components/angular-base64/angular-base64',
    'angular-strap': '../bower_components/angular-strap/dist/angular-strap',
    'angular-blocks': '../bower_components/angular-blocks/src/angular-blocks',
    'angular-ui-select2': '../bower_components/angular-ui-select2/src/select2',
    'angular-animate': '../bower_components/angular-animate/angular-animate',
    'angular-busy': '../bower_components/angular-busy/dist',
    'ng-infinite-scroll': '../bower_components/ngInfiniteScroll/build/ng-infinite-scroll',
    'ng-grid': '../bower_components/ng-grid',

    // XXX "../bower_components/ist-commun-webui" works only for the client app
    'de-metr': '../bower_components/ist-commun-webui/app/de-metr'
  },
  shim: {
    'bootstrap.affix': {deps: ['jquery']},
    'bootstrap.alert': {deps: ['jquery']},
    'bootstrap.button': {deps: ['jquery']},
    'bootstrap.carousel': {deps: ['jquery']},
    'bootstrap.transition': {deps: ['jquery']},
    'bootstrap.collapse': {deps: ['jquery']},
    'bootstrap.dropdown': {deps: ['jquery']},
    'bootstrap.modal': {deps: ['jquery']},
    'bootstrap.scrollspy': {deps: ['jquery']},
    'bootstrap.tab': {deps: ['jquery']},
    'bootstrap.tooltip': {deps: ['jquery']},
    'bootstrap.popover': {deps: ['jquery', 'bootstrap.tooltip']},
    'bootstrap-hover-dropdown': {deps: ['jquery', 'bootstrap.dropdown']},
    'bootstrap-datepicker': {deps: ['jquery']},
    'bootstrap-datepicker.el': {deps: ['jquery', 'bootstrap-datepicker']},

    'jquery-backstretch': {deps: ['jquery']},
    'jquery-migrate': {deps: ['jquery']},
    'jquery-blockUI': {deps: ['jquery']},
    'jquery-cookie': {deps: ['jquery']},
    'jquery-uniform': {deps: ['jquery']},
    'jquery-validate': {deps: ['jquery']},
    'jquery-validate.el': {deps: ['jquery-validate']},

    'select2': {deps: ['jquery', 'css!de-metr/select2_metro-minimized']},

    'angular-file-upload-html5-shim': {deps: []},
    'angular': {deps: ['angular-file-upload-html5-shim', 'jquery'], exports: 'angular'},
    'angular-locale_el': {deps: ['angular']},
    'angular-animate': {deps: ['angular']},
    'angular-file-upload': {deps: ['angular-file-upload-html5-shim', 'angular', 'angular-locale_el' /*TODO Move angular-locale_el dependency on code which depends on greek localization to work properly */]},
    'angular-route': {deps: ['angular']},
    'angular-mocks': {deps: ['angular']},
    'angular-resource': {deps: ['angular']},
    'angular-cookies': {deps: ['angular']},
    'angular-sanitize': {deps: ['angular']},
    'angular-base64': {deps: ['angular']},
    'angular-blocks': {deps: ['angular']},
    'angular-strap': {deps: ['angular', 'bootstrap-datepicker', 'bootstrap-datepicker.el', 'bootstrap-hover-dropdown', 'bootstrap.transition', 'bootstrap.modal', 'bootstrap.tooltip']},
    'angular-ui-select2': {deps: ['angular', 'select2']},
    'angular-busy/angular-busy': {deps: ['angular', 'angular-animate', 'css!angular-busy/angular-busy']},
    'ng-infinite-scroll': {deps: ['angular']},
    'ng-grid/build/ng-grid': {deps: ['angular', 'css!ng-grid/ng-grid']},

  },
  enforceDefine: false
});
