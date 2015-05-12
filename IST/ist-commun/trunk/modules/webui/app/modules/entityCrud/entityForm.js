/*globals define*/
define(['angular',
  './entityCrudUtil',
  'text!./entityForm.html',
  'jquery',
  './selectFrom',
  './directives',
  'angular-route',
  'angular-resource',
  'angular-sanitize',
  '../support/dialogs/dialogs',
  '../core'], function(angular, util, entityFormTemplate, $) {
  'use strict';

  var module = angular.module('entityCrud.form', ['ngResource', 'ngRoute', 'core', 'dialogs', 'entityCrud.directives', 'entityCrud.selectFrom', 'ngSanitize']);

  module.run(['$templateCache', function($templateCache) {
    $templateCache.put('entityCrud/entityForm.html', entityFormTemplate);
  }]);

  /**
   * Prerequisites:
   * entityAPI, entityDefinition, entityForm to exist on a parent scope
   */
  module.controller('EntityFormCtrl', ['$scope', '$location', '$window', '$routeParams', 'apiService', 'dialogsService', 'EntityDefinitions', '$q', 'select2Fn', '$sce', '$timeout',
    function($scope, $location, $window, $routeParams, apiService, dialogsService, EntityDefinitions, $q, select2Fn, $sce, $timeout) {

    /**
     * Handle events that navigate away from forms with unsaved changes.
     * @param event
     * @returns {string}
     */
    (function() {
      var message = 'Υπάρχουν αλλαγές που δεν έχουν αποθηκευτεί.';

      $window.onbeforeunload = function (event) {
        if (!($scope.entityForm && $scope.entityForm.$dirty)) {
          return;
        }
        if (typeof event === 'undefined') {
          event = $window.event;
        }
        if (event) {
          event.returnValue = message;
        }
        return message;
      };

      $scope.$on('$locationChangeStart', function(event, newUrl) {
        if ($scope.entityForm && $scope.entityForm.$dirty &&
            ($scope.entityForm.aborted === undefined || $scope.entityForm.aborted === false)) {
          event.preventDefault();
          dialogsService.confirm({
            title: 'Επιβεβαίωση περιήγησης',
            content: '<strong>'+message+'.</strong><br><br>Θέλετε να τις ακυρώσετε και να κλείσετε τη φόρμα;',
            confirmActionText: 'Ναι, να ακυρωθούν',
            cancelActionText: 'Όχι, να παραμείνω εδώ',
            onConfirm: function() {
              $scope.entityForm.aborted = true;
              $window.location = newUrl;
            }
          });
          return;
        }
        $window.onbeforeunload = null;
      });
    })();

    $scope.formulaValue = function(fieldDefinition, arg1, arg2) {
      return fieldDefinition.formula ?
          ((fieldDefinition.type && fieldDefinition.type === 'html') ? $sce.getTrustedHtml(fieldDefinition.formula(arg1, arg2)) :
              fieldDefinition.formula(arg1, arg2)) : null;
    };

    $scope.getActionUrl = util.getActionUrlFn($scope);

    $scope.removeListItem = function(list, index) {
      list.splice(index, 1);
      $scope.entityForm.$setDirty();
    };

    $scope.moveListItem = function(list, index, delta) {
      if (index < 0 || index > list.length || index + delta > list.length || index + delta < 0 || delta === 0) {
        return;
      }
      var cutItems = list.splice(index, 1);
      list.splice(index+delta, 0, cutItems[0]);
      $scope.entityForm.$setDirty();
    };

    $scope.addListItem = function(listParent, listFieldName) {
      if (!listParent[listFieldName]) {
        listParent[listFieldName] = [];
      }
      listParent[listFieldName].push({});
      $scope.entityForm.$setDirty();
    };

    $scope.fieldValue = util.fieldValueFn(EntityDefinitions);

    $scope.isNew = /new$/.test($location.path());

    $scope.$watch('entityDefinition.entityDisplayName ? entityDefinition.entityDisplayName(entity) : entity.name', function(name) {
      $window.document.title = $scope.entityDefinition.name.singular + ' ' + ($scope.isNew ? '- Νέα εγγραφή' : name) + ' - HCAA AVIS';
    });

    function createResource(entityDefinition) {
      var entity = {$resolved: false};
      var deferred = $q.defer();
      var promises = [deferred.promise];

      angular.forEach($routeParams, function(itemValue, itemKey) {
        if (entityDefinition.fieldsMap[itemKey] && entityDefinition.fieldsMap[itemKey].selectFrom && entityDefinition.fieldsMap[itemKey].selectFrom.resourceApi) {
          promises.push(entityDefinition.fieldsMap[itemKey].selectFrom.resourceApi.get({identifier: itemValue}).$promise.then(function(referencedEntity) {
            entity[itemKey] = referencedEntity;
          }));
        } else {
          entity[itemKey] = itemValue;
        }
      });
      if (entityDefinition.events && entityDefinition.events.onPreCreate) {
        entityDefinition.events.onPreCreate(entity);
      }
      deferred.resolve(entity);
      entity.$promise = $q.all(promises).then(function(){
        entity.$resolved = true;
      });
      return entity;
    }

    if ($scope.isNew) {
      $scope.entity = createResource($scope.entityDefinition);
    } else {
      $scope.entity = $scope.entityAPI.get({identifier: $routeParams.identifier});
    }

    $scope.entity.$promise = $scope.entity.$promise.then(apiService.retrocycle);

    if ($scope.entityDefinition.ngController) {
      $scope.entity.$promise = $scope.entity.$promise.then(function(/*entity*/) {
        $scope.entityDefinition.ngController($scope);
      });
    }

    function apiErrorHandler(httpResponse) {
      var errorMessage,
        defaultSuggestionMessage = 'Παρακαλώ δοκιμάστε πάλι αργότερα. Αν επαναλαμβάνεται το σφάλμα καλέστε τεχνική υποστήριξη.';
      switch(httpResponse.status)
      {
        case 0:
          errorMessage = 'Ο διακομιστής δεν απάντησε ακόμα και δεν είναι γνωστό αν ολοκληρώθηκε η ενέργεια. '+defaultSuggestionMessage;
          break;
        case 409:
          if (httpResponse.config.method === 'DELETE') {
            errorMessage = 'Δεν μπορεί να γίνει η διαγραφή διότι η εγγραφή συσχετίζεται με άλλα στοιχεία';
          } else {
            errorMessage = 'Δεν μπορεί να γίνει η ενέργεια διότι θα παραβίαζε την ακεραιότητα της βάσης δεδομένων';
          }
          break;
        case 400:
          errorMessage = 'Δεν μπορεί να γίνει η ενέργεια επειδή τα στοιχεία που δώσατε δεν είναι έγκυρα';
          break;
        case 403:
          errorMessage = 'Δεν μπορεί να γίνει η ενέργεια επειδή δεν έχετε τα απαιτούμενα δικαιώματα. Επικοινωνήστε με τον διαχειριστή του συστήματος για να αποκτήσετε πρόσβαση.';
          break;
        default:
          errorMessage = 'Συνέβη ένα σφάλμα κατά την επικοινωνία με τον διακομιστή. '+defaultSuggestionMessage;
      }

      $scope.formErrors={ title : errorMessage, list : []};

      function findField(fieldName, fieldDefinitions) {
//        console.debug('findField', fieldName, fieldDefinitions);
        if (!(fieldName && fieldDefinitions)) {
          return undefined;
        }
        for (var i=0; i<fieldDefinitions.length; i++) {
          var field=fieldDefinitions[i];
          if (field.name === fieldName) {
            return field;
          } else if (field.name && fieldName && (field.name === fieldName.substring(0, field.name.length))) {
            return findField(fieldName.substring(field.name.length).replace(/^\[\d+\]?\./, ''), field.embedded);
          }
        }
      }

      if (httpResponse.data && httpResponse.data.errors) {
        $scope.formErrors.list=httpResponse.data.errors.filter(function(error) {
          error.field = findField(error.property, $scope.entityDefinition.fields);
          return error;
        });
      }
    }

    /**
     * Transforms the input entity to a form that can be sent to REST API for persistence
     * @param source an entity
     * @returns {{}} the transformed entity
     */
    function transformScopeObjectToRestRequestObject(source, parentPath) {
      function findFieldDef(fields, fullPathName) {
        for(var i=0; i<fields.length; i++) {
          if (fields[i].name === fullPathName) {
            return fields[i];
          } else if (fields[i].name && fields[i].embedded && (fields[i].name + '.') === fullPathName.substring(0, fields[i].name.length + 1)) {
            return findFieldDef(fields[i].embedded, fullPathName.substring(fields[i].name.length + 1));
          }
        }
        return undefined;
      }
      function transformFieldValue(sourceFieldValue) {
        if (angular.isArray(sourceFieldValue)) {
          var transformedArray = [];
          sourceFieldValue.forEach(function(element) {
            var transformedElement = transformFieldValue(element);
            if (transformedElement !== undefined) {
              transformedArray.push(transformedElement);
            }
          });
          return transformedArray;
        } else if (sourceFieldValue && sourceFieldValue.hasOwnProperty('id')) {
          // It is a referenced entity or an enum
          var fullPathName = parentPath ? parentPath+'.'+name : name;
          var foundFieldDef = findFieldDef($scope.entityDefinition.fields, fullPathName);
          if (!foundFieldDef) {
            console.warn('Field not saved because it cannot be found in the entityDefinition: ', fullPathName);
            return undefined;
          }
          var selectFrom = foundFieldDef.selectFrom;
          if (!selectFrom) {
            console.warn('Field not saved because selectFrom property cannot be found in the corresponding fieldDefinition: ', fullPathName);
            return undefined;
          }
          if (selectFrom.isValueObject) {
            return sourceFieldValue;
          } else if (selectFrom.url === undefined) {
            // it's just a simple enum (option list)
            return sourceFieldValue.id;
          } else {
            // it's a referenced entity
            // XXX here I assume that the url path of the entity of the client is the same as on the server
            return apiService.apiPathname + selectFrom.url + '/' + sourceFieldValue.id;
          }
        } else if (sourceFieldValue && sourceFieldValue.hasOwnProperty('_links')) {
          return sourceFieldValue._links.self.href.match(/.*(\/api\/.*)/)[1];
        } else if (angular.isObject(sourceFieldValue) && !angular.isArray(sourceFieldValue)) {
          if (angular.equals(sourceFieldValue, {}) && source[name+'$org$'] === null) {
            return null;
          }
          return transformScopeObjectToRestRequestObject(sourceFieldValue, name);
        } else if (sourceFieldValue === '') {
          // Empty string same is equivalent to null to allow user to clear nullable fields
          return null;
        } else {
          return sourceFieldValue;
        }
      }
      var target = {};
      for(var name in source) {
        // $promise, $resolve are coming from $resource
        // $org$ keeps the original value for the field
        if (!source.hasOwnProperty(name) || name === '$promise' || name === '$resolved' || name.indexOf('$org$') !== -1) {
          continue;
        }
        var targetValue = transformFieldValue(source[name], [name]);
        if (targetValue !== undefined) {
          target[name] = targetValue;
        }
      }
//      console.debug('transformed '+parentPath+' object from ', source, 'to', target);
      return target;
    }

    $scope.getEntityFieldLeafParentScope = function(path, parent) {
      var segs = path.split('.');
      if (!parent) {
        parent = $scope.entity;
      }
      while (segs.length > 1) {
        var pathStep = segs.shift();
        if (parent[pathStep] === undefined || parent[pathStep] === null) {
          // $org$ keeps the original value for the field
          parent[pathStep+'$org$'] = null;
          parent[pathStep] = {};
        }
        parent = parent[pathStep];
      }
      //console.debug('getFieldModel', parent);
      return parent;
    };

    $scope.getEntityFieldLeafName = function(path) {
      var segs = path.split('.');

      var result = segs[segs.length-1];
      return result;
    };

    $scope.setFocus = function(error) {
      if (!(error && error.field && error.field.name)) {
        return;
      }
      var field = error.field;
      var el;
      if (field.selectFrom === undefined) {
        el = $('input[name="'+error.property+'"]');
        el.focus();
        el.select();
      } else {
        el = $('input[name="'+error.property+'"]');
        el.select2('open');
      }
    };

    $scope.getFormUrlForReferencedEntity = util.getFormUrlForReferencedEntity;

    function createDeferredSubmission(aScope) {
      var deferredSubmission = $q.defer();

      if (aScope) {
        $scope.entityForm.submitInProgress = true;
        aScope.submissionPromise = deferredSubmission.promise;
      }

      deferredSubmission.promise.finally(function() {
//        console.debug('deferredSubmission promise fulfilled');
        if (aScope) {
          delete aScope.entityForm.submitInProgress;
          delete aScope.submissionPromise;
        }
      });

      return deferredSubmission;
    }

    $scope.save = function(successCallback) {
      function onSuccessFn(resource, responseHeaders) {
        $scope.formErrors = undefined;
        $scope.entityForm.$setPristine();
        if ($scope.isNew && $scope.entityDefinition.events && $scope.entityDefinition.events.onPostCreate) {
          var location = responseHeaders('location');
          var entityId = location.substring(location.lastIndexOf('/') + 1);
          $scope.entityDefinition.events.onPostCreate($scope.entity, entityId, successCallback);
          return;
        } else if (!$scope.isNew && $scope.entityDefinition.events && $scope.entityDefinition.events.onPostUpdate) {
          $scope.entityDefinition.events.onPostUpdate($scope.entity, successCallback);
          return;
        }
        successCallback();
      }

      if ($scope.entityForm.submitInProgress) {
        return;
      }

      var deferredSubmission = createDeferredSubmission($scope);
      deferredSubmission.promise.catch(function() {
        $timeout(function() {
          $('button[type="submit"]').focus();
        }, true);
      });
      try {
        $scope.entityForm.submitted = true;
        if ($scope.entityDefinition.validator) {
          var validationErrors = $scope.entityDefinition.validator($scope.entity);
          if (validationErrors) {
            $scope.formErrors={ title : 'Η ενέργεια δε μπορεί να πραγματοποιηθεί', list : []};
            for (var k=0; k<validationErrors.length; k++) {
              for (var i=0; i<$scope.entityDefinition.fields.length; i++) {
                var field=$scope.entityDefinition.fields[i];
                if (field.name === validationErrors[k].fieldName) {
                  $scope.formErrors.list.push({field:field, message: validationErrors[k].errorMessage});
                }
              }
            }
            deferredSubmission.reject();
            return validationErrors;
          }
        }
        if ($scope.entityForm.$invalid) {
          if (!$scope.formErrors) {
            $scope.formErrors={ title : 'Υπάρχουν προβλήματα στη φόρμα. Δείτε τις σχετικές ενδείξεις. Παρακαλώ διορθώστε τα και ξαναδοκιμάστε.'};
          }
          deferredSubmission.reject();
          return;
        }
        var data = transformScopeObjectToRestRequestObject($scope.entity);
        if ($scope.isNew) {
          $scope.entityAPI.create(data, onSuccessFn, apiErrorHandler).$promise.then(deferredSubmission.resolve, deferredSubmission.reject);
        } else {
          delete $scope.entity.links;
          // TODO: Send only the modified fields
          $scope.entityAPI.update({identifier: $routeParams.identifier}, data, onSuccessFn, apiErrorHandler).$promise.then(deferredSubmission.resolve, deferredSubmission.reject);
        }
      } catch(e) {
        console.error('A exception has occured when trying to submit', e);
        deferredSubmission.reject(e);
      }

    };

    $scope.close = function() {
      if ($window.history.length === 1) {
        $window.close();
      }
      $window.history.back();
    };

    $scope.remove = function() {
      if (!$scope.isNew) {
        dialogsService.confirm({
          title: 'Επιβεβαίωση Διαγραφής',
          content: 'Πρόκειται να <strong>διαγραφεί οριστικά</strong> η εγγραφή. Σίγουρα;',
          confirmActionText: 'Ναι, να διαγραφεί',
          cancelActionText: 'Όχι, άκυρο',
          onConfirm: function() {
            var deferredSubmission = createDeferredSubmission($scope);
            try {
              $scope.entity.$delete({identifier: $routeParams.identifier}, $scope.close, apiErrorHandler).then(deferredSubmission.resolve, deferredSubmission.reject);
            } catch(e) {
              console.error('A exception has occured when trying to submit', e);
              deferredSubmission.reject(e);
            }
          }
        });
      }
    };

    $scope.select2 = select2Fn($scope.entity, $scope, 'entityForm', apiService.retrocycle, true /*Allow Editing*/);

    /**
     * Converts from angular dateFilter format to the format required by angular-strap datePicker
     * @param format
     * @returns {string}
     */
    $scope.datepickerDateFormat = function(format) {
      return format.replace(/MM/, 'mm');
    };

    $scope.entityFormFields = util.entityViewFieldsFn('formView');

  }]);

  return module;
});