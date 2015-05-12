/*globals define*/
define(function() {
  'use strict';

  function getFieldPathValue(obj, fieldPath) {
    if (fieldPath === undefined) {
      return null;
    }
    for (var i=0, path=fieldPath.split('.'), len=path.length; i<len; i++) {
      obj = obj[path[i]];
      if (obj === null || obj === undefined) {
        break;
      }
    }
    return obj;
  }

  return {
    getFormUrlForReferencedEntity: function (entity, fieldDefinition, referenceId) {
      if (fieldDefinition.selectFrom && fieldDefinition.selectFrom.url) {
        referenceId = referenceId || (entity[fieldDefinition.name] || {}).id;
        return '#/'+fieldDefinition.selectFrom.url + '/' +
          (referenceId === undefined ? 'new' : referenceId+'/edit');
      }
      return '';
    },
    getFieldPathValue: getFieldPathValue,
    fieldValueFn: function(EntityDefinitions, $sce, fieldDefinitionMap) {
      return function(entity, fieldDefinitionOrName) {
        var fieldDefinition = typeof fieldDefinitionOrName !== 'string' ? fieldDefinitionOrName : fieldDefinitionMap ? fieldDefinitionMap[fieldDefinitionOrName] : undefined;
        if (fieldDefinition === undefined) {
          console.error('Missing field '+fieldDefinitionOrName+' in fieldDefinitionMap', fieldDefinitionMap);
          return getFieldPathValue(entity, fieldDefinitionOrName);
        }
        var fieldValue = fieldDefinition.formula ?
            (($sce && fieldDefinition.type && fieldDefinition.type === 'html') ? $sce.getTrustedHtml(fieldDefinition.formula(entity)) :
                fieldDefinition.formula(entity)) : getFieldPathValue(entity, fieldDefinition.name);
        return EntityDefinitions.formatFieldValue(fieldValue, fieldDefinition);
      };
    },
    entityViewFieldsFn: function(viewName) {
      return function(entityDefinition, entity) {
        if (entityDefinition[viewName] && entityDefinition[viewName].fields) {
          return (angular.isFunction(entityDefinition[viewName].fields) ? entityDefinition[viewName].fields(entity) : entityDefinition[viewName].fields)
            .map(function(item) {
              return angular.isString(item) ? entityDefinition.fieldsMap[item] : item;
            });
        }
        return entityDefinition.fields;
      };
    },
    getActionUrlFn: function ($scope) {
      return function getActionUrl(action) {
        var actionUrl = action === undefined ? 'new' : angular.isFunction(action.url) ? action.url($scope.entity) : action.url;
        if (!actionUrl) {
          return '';
        }
        if (actionUrl.substring(0,4) === 'http') {
          return actionUrl;
        }
        if (actionUrl.substring(0,1) !== '/') {
          actionUrl = '/' + $scope.entityDefinition.url+'/'+actionUrl;
        }
        return '#'+actionUrl;
      };
    }
  };
});