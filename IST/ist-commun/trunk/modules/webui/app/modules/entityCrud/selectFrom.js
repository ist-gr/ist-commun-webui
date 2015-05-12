/**
 * Created by antonis on 31/1/14.
 */
/*globals define*/
define(['angular',
  'jquery',
  'angular-ui-select2'], function(angular, $) {
  'use strict';

  var module = angular.module('entityCrud.selectFrom', ['ui.select2']);

  function entityLookupTextFn(entityDefinition) {
    return entityDefinition.entityDisplayName ||
      function(entity) {
        return entity && entity.name ? entity.name : '';
      };
  }

  function getSelectFromItem(selectFrom, fieldValue) {
    if (Array.isArray(selectFrom) && selectFrom.length > 0 && selectFrom[0].hasOwnProperty('text')) {
      var selectFromItem = selectFrom.filter(function(item){
        return item.id === fieldValue;
      });
      if (selectFromItem.length === 1) {
        return selectFromItem[0];
      }
    }
    return { id: fieldValue, text: fieldValue };
  }

  function select2Fn(rootObject, scope, form, retrocycle/*, allowEditing*/) {

    return function(fieldDefinition, select2Options, nestedObject /*to support embedded objects*/) {
      // To understand what the heck is happening in this section one
      // must study http://ivaynberg.github.io/select2/
      var selectFrom = fieldDefinition.selectFrom;
      if (selectFrom === undefined) {
        return;
      }

      function enumToSelectDataFn(option) {
        if (option.hasOwnProperty('id') && option.hasOwnProperty('text')) {
          return option;
        }
        return {id: option, text: option};
      }

      function initSelectionFn(element, callback) {
        var fieldVal = (nestedObject || rootObject)[fieldDefinition.name];
//        console.debug('initSelection', 'element', element, 'fieldVal', fieldVal, 'element.val()', element.val(), 'element.select2("data")', element.select2('data'));
        if (fieldVal === undefined || fieldVal === null) {
          callback(undefined);
          return;
        }
        // The following is a trick to initialize the control value
        if (element.select2('data') === null) {
          // if the field is not a reference, we convert the enum value to select2 object
          callback(fieldVal.hasOwnProperty('id') ? fieldVal : getSelectFromItem(selectFrom, fieldVal));
          // Invoking $setPristine as a Workaround for https://github.com/angular-ui/angular-ui-OLDREPO/issues/268
          scope[form].$setPristine();
          return;
        }
        callback(element.select2('data'));
        return;
      }

      var result;
      var degree = function(fieldDefinition) {return (fieldDefinition.multiValue ? 'plural' : 'singular'); };
/*
      function formatterFn(entityDefinition, allowEditing, multiValue) {
        var textFunction = entityLookupTextFn(entityDefinition);
        return function(entity) {
          console.debug('formatter', this);
          var text = textFunction(entity);
          if (!allowEditing) {
            return text;
          }
          var editLink = '<a href="#/airports/e85e9f25-d95d-1b86-e040-a8c0173c17a1/edit" onclick="window.open(\'#/airports/e85e9f25-d95d-1b86-e040-a8c0173c17a1/edit\', \'_self\')" style="float: right"><i class="fa fa-pencil"></i></a>';
          if (multiValue) {
            return text + editLink;
          }
          return text +
                '<a href="#/airports/new" onclick="window.open(\'#/airports/new\', \'_self\')" style="float: right"><i class="fa fa-plus"></i></a>' + editLink; // TODO
        };
      }
*/
      if (angular.isArray(selectFrom.data || selectFrom)) {
        result = {
          placeholder: select2Options && select2Options.placeholder ? select2Options.placeholder : 'Επιλέξτε από τη λίστα',
          allowClear: true,
          formatNoMatches: function (/*term*/) {
            return 'Δεν βρέθηκε.';
          },
          initSelection: initSelectionFn
        };
        var arrayData = selectFrom.data || selectFrom;
        result.data = arrayData.map(enumToSelectDataFn);
        if (selectFrom.formatResult) {
          result.formatResult = selectFrom.formatResult;
        }
        if (selectFrom.formatSelection) {
          result.formatSelection = selectFrom.formatSelection;
        }
        if (selectFrom.formatResult || selectFrom.formatSelection) {
          result.escapeMarkup = function(m) { return m; };
        }
      } else if (angular.isFunction(selectFrom) || angular.isFunction(selectFrom.data)) {
        result = {
          placeholder: select2Options && select2Options.placeholder ? select2Options.placeholder : 'Επιλέξτε από τη λίστα',
          allowClear: true,
          formatNoMatches: selectFrom.formatNoMatches || function (/*term*/) {
            return 'Δεν βρέθηκε.';
          },
          formatSearching: function() {
            return 'Γίνεται αναζήτηση...';
          },
          formatLoadMore: function() {
            return 'Φόρτωση περισσότερων...';
          },
          initSelection: initSelectionFn,
          query: function(options) {
            var result = angular.isFunction(selectFrom) ? selectFrom() : selectFrom.data();
            if (angular.isArray(result)) {
              result = {
                results: result.map(enumToSelectDataFn),
                more: false
              };
            }
            options.callback(result);
          }
        };
        if (selectFrom.formatResult) {
          result.formatResult = selectFrom.formatResult;
        }
        if (selectFrom.formatSelection) {
          result.formatSelection = selectFrom.formatSelection;
        }
        if (selectFrom.formatResult || selectFrom.formatSelection) {
          result.escapeMarkup = function(m) { return m; };
        }
      } else {
        result = {
          placeholder: select2Options && select2Options.placeholder ? select2Options.placeholder : 'Επιλέξτε '+
            (selectFrom.name[degree(fieldDefinition)+'Accusative'] ? selectFrom.name[degree(fieldDefinition)+'Accusative'] : selectFrom.name[degree(fieldDefinition)]), /* +
            (allowEditing && !fieldDefinition.multiValue ? '<a href="#/airports/new" onclick="window.open(\'#/airports/new\', \'_self\')" style="float: right"><i class="fa fa-plus"></i></a>' : ''),*/
          quietMillis: 100,
          allowClear: true,
          loadMorePadding:51,
          formatNoMatches: function(/*term*/) {
            return 'Δεν βρέθηκε.'; // + (allowEditing && fieldDefinition.multiValue ? '<a href="#/airports/new" onclick="window.open(\'#/airports/new\', \'_self\')" style="float: right"><i class="fa fa-plus"></i></a>' : ''); // TODO
          },
          formatSearching: function() {
            return 'Γίνεται αναζήτηση...';
          },
          formatLoadMore: function() {
            return 'Φόρτωση περισσότερων...';
          },
          formatResult: entityLookupTextFn(selectFrom),
          formatSelection: /*formatterFn(selectFrom, allowEditing, fieldDefinition.multiValue),*/entityLookupTextFn(selectFrom),
          initSelection: initSelectionFn,
          query: function(options) {
            var result = {},
              selectFromFinder = angular.isFunction(fieldDefinition.selectFromFinder) ? fieldDefinition.selectFromFinder(rootObject, nestedObject) : {name: fieldDefinition.selectFromFinder},
              resourceActionName = selectFromFinder.name || 'query',
              resourceActionParams = angular.extend({page: options.page - 1, size: 10, q: options.term}, selectFromFinder.params);

            selectFrom.resourceApi[resourceActionName](resourceActionParams, function(resource) {
              result.results = angular.isArray(resource) ? resource : resource.content;
              result.results = retrocycle(result.results);
              var page = angular.isArray(resource) ? {} : resource.page ? resource.page : resource;
              result.more = page.number === undefined ? false : page.number < (page.totalPages - 1);
              if (selectFromFinder.transformResults) {
                result.results = selectFromFinder.transformResults(result.results);
              }
              var selectedData = options.element.select2('data');
              if (selectedData && selectedData.id && options.page === 1 && !options.term) {
                if (!result.results.some(function(item) {return (item && item.id === selectedData.id);})) {
                  result.results.unshift(selectedData);
                }
              }
              options.callback(result);
            }, function(httpResponse) {
              console.error(httpResponse);
              result.results = [];
              result.more = false;
              options.callback(result);
            });
          }
        };
//        if (allowEditing) {
//          result.escapeMarkup = function(m) { console.debug('escapeMarkup', this); return m; };
//        }
      }

      if (fieldDefinition.multiValue) {
        result.multiple = true;
        result.closeOnSelect = false;
      }

      return result;
    };
  }

  //fix modal force focus (keyboard typing did not work in select2 in modal dialogs)
  $.fn.modal.Constructor.prototype.enforceFocus = function () {
    var that = this;
    $(document).on('focusin.modal', function (e) {
      if ($(e.target).hasClass('select2-input')) {
        return true;
      }

      if (that.$element[0] !== e.target && !that.$element.has(e.target).length) {
        that.$element.focus();
      }
    });
  };

  module.factory('select2Fn', [function() {
    return select2Fn;
  }]);

  module.factory('select2Service', [function() {
    return {
      select2Fn: select2Fn,
      getSelectFromItem: getSelectFromItem,
      entityLookupTextFn: entityLookupTextFn
    };
  }]);

  return module;
});