/*global define */
define(['angular',
  'text!./confirm.html',
	'angular-strap',
	'angular-sanitize'], function (angular, htmlTemplate) {
	'use strict';

	// TODO: consider using https://github.com/m-e-conroy/angular-dialog-service

	var module = angular.module('dialogs', ['$strap', 'ngSanitize']);

  module.run(['$templateCache', function($templateCache) {
    $templateCache.put('dialogs/confirm.html', htmlTemplate);
  }]);
	
	module.factory('dialogsService', ['$modal', '$q', '$rootScope', '$sce', function($modal, $q, $rootScope, $sce) {
		function modalCallbackFn(scope, nextAction) {
			return function($modal) {
				scope.actionWasSelected = true;
				$modal('hide');
        if (nextAction !== undefined) {
          nextAction();
        }
			};
		}
		return {
			/**
			 * Shows a modal confirmation dialog with a confirmation and a cancel
			 * button. Invokes specified callback when user confirms or cancels.
			 *
			 * Supports:
			 * <ul>
			 *   <li>HTML markup in content</li>
			 *   <li>Keyboard navigation (enter, escape)</li>
			 *   <li>Optional onCancel callback</li>
			 * </ul>
			 *
			 * @param options
			 * { title: String, content: String, confirmActionText: String, cancelActionText: String, onConfirm: function, onCancel: function }
			 */
			confirm: function(options) {
				var scope = $rootScope.$new();
				var modalOptions = angular.copy(options);
				modalOptions.onCancel = modalCallbackFn(scope, options.onCancel);
				modalOptions.onConfirm = modalCallbackFn(scope, options.onConfirm);
				modalOptions.content = $sce.getTrustedHtml(options.content);
				scope.modal = modalOptions;
				scope.$on('modal-hidden', function(evt) {
					if (evt.currentScope === undefined || evt.currentScope.actionWasSelected === undefined) {
						if (options.onCancel !== undefined) {
							options.onCancel();
						}
					}
				});
				// Create modal (returns a promise since it may have to perform an http request)
				var promise = $modal({template: 'dialogs/confirm.html', persist: false, show: false, backdrop: 'static', keyboard: true, scope: scope});
				// XXX Workaround to make enter key work
				var primaryButton;
				scope.$on('modal-shown', function(evt) {
					if (primaryButton !== undefined) {
						primaryButton.focus();
					}
				});
				$q.when(promise).then(function(modalEl) {
					modalEl.modal('show');
					// XXX Workaround to make enter work
					primaryButton = modalEl.find('button[type="submit"]').first();
				});
			},
      /**
       * Opens the specified template as a modal dialog and adds a $dismissModal function on the specified scope so that
       * the opened dialog can be closed.
       * @param options {templateUrl, scope}
       */
      showModal: function(options) {
        var scope = options.scope.$new();
        var promise = $modal({template: options.templateUrl, persist: false, show: false, backdrop: 'static', keyboard: true, scope: scope});
        $q.when(promise).then(function(modalEl) {
          modalEl.modal('show');
          options.scope.$dismissModal = function() {
            modalEl.modal('hide');
          }
        });
      }
		};
	}]);

	return module;
});
