/**
 * Created by antonis on 31/1/14.
 */
/*globals define*/
define(['angular',
  './entityList',
  './entityForm',
  './restApiHttp400ToExcel',
  './selectFrom',
  './directives',
  'angular-route',
  'angular-resource',
  'angular-sanitize',
  '../support/dialogs/dialogs',
  'core'], function(angular) {
  'use strict';

  var module = angular.module('entityCrud', ['entityCrud.form', 'entityCrud.list', 'ngResource', 'ngRoute', 'core', 'dialogs', 'entityCrud.directives', 'entityCrud.selectFrom', 'ngSanitize']);

  module.factory('EntityDefinition', function() {
    return {
      fieldPatterns: {
        FOUR_DIGIT_YEAR: {pattern: '/^[0-9]{4}$/', patternMessage: 'Πρέπει να είναι τετραψήφιο'},
        JODA_PERIOD_MONTHS_DAYS: {pattern: '/^P(\\d+Y)?(\\d+M)?(\\d+D)?$/', patternMessage: 'π.χ. P12D (12 Days) ή P1M5D (1 Month and 5 Days) ή P3Y2M15D (3 Years and 2 Months and 15 Days)'},
        HOUROFDAY: {pattern: '/^[0-9]{2}:[0-9]{2}$/', patternMessage: 'Πρέπει να είναι της μορφής ΩΩ:ΛΛ'}
      }
    };
  });

  module.factory('EntityDefinitions', ['dateFilter', 'select2Service', 'NumberFormatter', function(dateFilter, select2Service, NumberFormatter) {
    var entityDefinitionsByDomainClassName = {},
      entityDefinitionsByUrl = {};

    function addFieldsMap(entityDefinition) {
      if (entityDefinition.fieldsMap === undefined) {
        entityDefinition.fieldsMap = {};
        entityDefinition.fields.forEach(function(fieldDef) {
          if (fieldDef.name) {
            if (entityDefinition.fieldsMap[fieldDef.name] !== undefined) {
              console.warn('Field with name '+fieldDef.name+' has been declared more than once in ', entityDefinition, 'Will use only the last occurrence');
            }
            entityDefinition.fieldsMap[fieldDef.name] = fieldDef;
          }
        });
      } else if (entityDefinition.fields === undefined) {
        entityDefinition.fields = [];
        for(var fieldName in entityDefinition.fieldsMap) {
          if (entityDefinition.fieldsMap.hasOwnProperty(fieldName)) {
            var f = entityDefinition.fieldsMap[fieldName];
            f.name = fieldName;
            entityDefinition.fields.push(f);
          }
        }
      }
      // populates "fieldsMap" property in advanced search
      if (entityDefinition.advancedSearch && entityDefinition.advancedSearch.fields) {
        if (entityDefinition.advancedSearch.fieldsMap === undefined) {
          entityDefinition.advancedSearch.fieldsMap = {};
        }
        entityDefinition.advancedSearch.fields.forEach(function(field) {
          if (angular.isString(field)) {
            entityDefinition.advancedSearch.fieldsMap[field] = entityDefinition.fieldsMap[field];
          } else {
            entityDefinition.advancedSearch.fieldsMap[field.name] = field;
          }
        });
      }
    }
    return {
      fieldsMap: function(entityDefinition) {
        if (!entityDefinition.fieldsMap) {
          addFieldsMap(entityDefinition);
        }
        return entityDefinition.fieldsMap;
      },
      register: function(entityDefinition) {
        if (entityDefinition.domainClassName !== undefined) {
          entityDefinitionsByDomainClassName[entityDefinition.domainClassName] = entityDefinition;
//          console.debug('Successfully registered entityDefinition with domainClassName '+entityDefinition.domainClassName);
        } else {
          console.error('Missing domainClassName from entityDefinition with url '+entityDefinition.url);
        }
        if (entityDefinition.url !== undefined) {
          entityDefinitionsByUrl[entityDefinition.url] = entityDefinition;
        } else {
          console.error('Missing url from entityDefinition', entityDefinition);
        }
        // XXX add a fieldsMap in all entityDefinitions (if not already there)
        addFieldsMap(entityDefinition);
      },
      getEntityDefinitionForDomainClass: function(domainClassName) {
        var entityDef = entityDefinitionsByDomainClassName[domainClassName];
        if(entityDef === undefined) {
          console.error('Missing entityDefinition for domainClassName:', domainClassName);
        }
        return entityDef;
      },
      findFieldDefinitionFor: function(entityDef, fieldPath) {
        var fieldDef = entityDef.fieldsMap[fieldPath];
        // XXX Quick n dirty way: fabricates fieldDef for detailed fields of TimePeriod (startDateTime, endDateTime)
        // TODO Refactor code so that it does not know about thing it shouldn't
        if (fieldDef === undefined) {
          var dateRangeMatch = fieldPath.match(/^([^\.]+)\.(start|end)DateTime/);
          if (dateRangeMatch) {
            fieldDef = entityDef.fieldsMap[dateRangeMatch[1]];
            if (fieldDef && fieldDef.type && fieldDef.type.dateRange) {
              return {
                name: fieldPath,
                label: fieldDef.label + ' / ' +
                  (dateRangeMatch[2] === 'start' ? 'Έναρξη' : 'Λήξη'),
                type: {dateTime: fieldDef.type.dateRange.format}
              };
            }
          }
        }
        return fieldDef;
      },
      getEntityDefinitionFromUrl: function(url) {
        var entityDef = entityDefinitionsByUrl[url];
        if(entityDef === undefined) {
          console.error('Missing entityDefinition for url:', url);
        }
        return entityDef;
      },
      getEntityDefinitionsAsSelectFromOptions: function() {
        var result=[];
        for(var def in entityDefinitionsByDomainClassName) {
          if (entityDefinitionsByDomainClassName.hasOwnProperty(def)) {
            result.push({id:def, text: entityDefinitionsByDomainClassName[def].name.singular});
          }
        }
        return result;
      },
      formatFieldValue: function(fieldValue, fieldDefinition) {
        function format(val, fieldDef) {
          var formattedValues;
          if (fieldDef.displayAs) {
            return fieldDef.displayAs(val);
          } else if (fieldDef.multiValue) {
            formattedValues = [];
            if (val === null || val === undefined) {
              return formattedValues;
            }
            if (angular.isArray(val)) {
              val.forEach(function(element) {
                formattedValues.push(format(element, fieldDef));
              });
              return '['+formattedValues.join(', ')+']';
            } else {
              fieldDef = fieldDef.embedded ? fieldDef.embedded : fieldDef;
              var i = 0;
              for (var prop in val) {
                formattedValues.push((fieldDef[i].label  || fieldDef[i].name) + ': ' + format(val[prop], fieldDef[i]));
                i++;
              }
              return '{'+formattedValues.join(', ')+'}';
            }

          } else {
            if (val === null || val === undefined) {
              return '';
            }
            var fieldType = fieldDef.type;
            if (fieldType && fieldType.dateTime) {
              // JSON format is expected to be in milliseconds
              return dateFilter(val, fieldType.dateTime);
            }
            if (fieldType && fieldType.dateRange) {
              return dateFilter(val.startDateTime, fieldType.dateRange.format) + ' - ' + (val.endDateTime === null ? '' : dateFilter(val.endDateTime, fieldType.dateRange.format));
            }
            if (fieldType && fieldType.numeric) {
              return NumberFormatter.format(val);
            }
            var selectFrom = fieldDef.selectFrom;
            if (selectFrom !== undefined) {
              if (Array.isArray(selectFrom)) {
                return select2Service.getSelectFromItem(selectFrom, val).text;
              }
              var mapFn = select2Service.entityLookupTextFn(selectFrom);

              return angular.isArray(val) ? val.map(mapFn).join(', ') : mapFn(val);
            }

            return val;
          }
        }
        if (fieldDefinition === undefined) {
          fieldDefinition = {};
        }
        return format(fieldValue, fieldDefinition);
      }
    };
  }]);

  /**
   * Configures $routeProvider, API and controller for specified module and entity
   */
  module.declareAngularStructuresForEntity = function(targetModule, entityName, options) {
    function calcUrlPath(name) {
      var plural = name.match(/y$/) ? name.replace(/y$/, 'ies') : name.match(/ss$/) ? name+'es' : name.match(/x$/) ? name+'es' : name+'s';
      return plural[0].toLowerCase() + plural.substring(1);
    }
    options = options || {};
    if (options.urlPath === undefined) {
      options.urlPath = calcUrlPath(entityName);
    }

    targetModule.config(['$routeProvider', function ($routeProvider) {
      var entityFormOptions = {
        controller: entityName+'Ctrl'
      };
      if (options.formTemplate === undefined) {
        entityFormOptions.templateUrl = 'entityCrud/entityForm.html';
      } else {
        entityFormOptions.template = options.formTemplate;
      }
      $routeProvider
        .when('/'+options.urlPath, {
          templateUrl: options.noListView ?
              'entityCrud/entityForm.html' :
              (options.listTemplateUrl ? options.listTemplateUrl : 'entityCrud/entityList.html'),
          controller: entityName+'Ctrl',
          reloadOnSearch: false
        })
        .when('/'+options.urlPath+'/:identifier/edit', entityFormOptions)
        .when('/'+options.urlPath+'/new', entityFormOptions);
    }]);

    targetModule.run([entityName+'Definition', 'EntityDefinitions', function(entityDefinition, EntityDefinitions) {
      EntityDefinitions.register(entityDefinition);
    }]);

    targetModule.factory(entityName+'API', ['$resource', 'apiService', function($resource, apiService) {
      var defaultTimeout = 10000;
      var actions = {
        get: {
          method: 'GET',
          url: apiService.apiPathname + options.urlPath + '/query/:identifier',
          timeout: defaultTimeout
        },
        query: {
          method: 'GET',
          url: apiService.apiPathname + options.urlPath + '/query',
          timeout: defaultTimeout
        },
        update: {
          method: 'POST',
          url: apiService.apiPathname + options.urlPath + '/:identifier?_method=PATCH',
          timeout: defaultTimeout
        },
        create: {
          method: 'POST',
          timeout: defaultTimeout
        },
        delete: {
          method: 'DELETE',
          timeout: defaultTimeout
        }
      };

      angular.forEach(options.resourceApi, function(customActionValue, customActionName) {
        if (actions.hasOwnProperty(customActionName)) {
          if (options.resourceApi[customActionName].url) {
            options.resourceApi[customActionName].url = apiService.apiPathname + options.resourceApi[customActionName].url;
          }
          angular.extend(actions[customActionName], customActionValue);

        } else {
          actions[customActionName] = customActionValue;
          actions[customActionName].url = apiService.apiPathname + options.urlPath + '/' + actions[customActionName].url;
          if (customActionValue.timeout === undefined) {
            actions[customActionName].timeout = defaultTimeout;
          }
        }
      });

      return $resource(apiService.apiPathname + options.urlPath+'/:identifier',
        {},
        actions
      );
    }]);

    targetModule.controller(entityName+'Ctrl', ['$scope', entityName+'API', entityName+'Definition', function($scope, entityAPI, entityDefinition) {
      $scope.entityDefinition = entityDefinition;
      $scope.entityAPI = entityAPI;
    }]);
  };

  return module;
});