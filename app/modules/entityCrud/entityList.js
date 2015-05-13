/*globals define*/
define(['angular',
  './entityCrudUtil',
  'text!./entityList.html',
  'text!./entityListImport.html',
  '../support/stringUtils',
  './restApiHttp400ToExcel',
  'saveAs',
  'appConstants',
  'jquery',
  './selectFrom',
  './directives',
  'angular-route',
  'angular-resource',
  'angular-sanitize',
  'support/dialogs/dialogs',
  'angular-file-upload',
  'ng-grid/build/ng-grid',
  'core'], function(angular, util, entityListTemplate, entityListImportTemplate, stringUtils, createXlsFile, saveAs, appConstants, $) {
  'use strict';

  var module = angular.module('entityCrud.list', ['ngResource', 'ngRoute', 'core', 'dialogs', 'entityCrud.directives', 'entityCrud.selectFrom', 'ngSanitize', 'ngGrid', 'angularFileUpload']);

  var entityListImportTemplateUrl = 'entityCrud/entityListImport.html';

  module.run(['$templateCache', function($templateCache) {
    $templateCache.put('entityCrud/entityList.html', entityListTemplate);
    $templateCache.put(entityListImportTemplateUrl, entityListImportTemplate);
  }]);

  /**
   * Prerequisites:
   * entityAPI, entityDefinition to exist on a parent scope
   */
  module.controller('EntityListCtrl', ['$scope', '$rootScope', 'apiService', 'EntityDefinitions', '$sce', '$window', '$location', 'select2Fn', 'dialogsService', '$filter', '$utilityService', '$timeout', '$http', function($scope, $rootScope, apiService, EntityDefinitions, $sce, $window, $location, select2Fn, dialogsService, $filter, $utilityService, $timeout, $http) {

//    var previousLocationSearchOptions;

    /**
     * Greek localization for ngGrid
     */
    $window.ngGrid.i18n.el = {
      ngAggregateLabel: 'εγγραφές',
      ngGroupPanelDescription: 'Σύρετε επικεφαλίδες στηλών εδώ για ομαδοποίηση.',
      ngSearchPlaceHolder: 'Αναζήτηση...',
      ngMenuText: 'Επιλέξτε Στήλες:',
      ngShowingItemsLabel: 'Εμφανίζονται: ',
      ngTotalItemsLabel: 'Εγγραφές: ',
      ngSelectedItemsLabel: 'Επιλεγμένες: ',
      ngPageSizeLabel: 'Μέγεθος Σελίδας:',
      ngPagerFirstTitle: 'Πρώτη Σελίδα',
      ngPagerNextTitle: 'Επόμενη Σελίδα',
      ngPagerPrevTitle: 'Προηγούμενη Σελίδα',
      ngPagerLastTitle: 'Τελευταία Σελίδα'
    };

    /**
     * HTML Templates for ngGrid
     */
    var numericFieldAlignment = 'ng-class="{\'numeric-field\' : col.colDef.fieldDef.type && col.colDef.fieldDef.type.numeric}"';
    var aggregateTemplate = '<div ng-click="row.toggleExpand()" ng-style="rowStyle(row)" class="ngAggregate">' +
        '<span class="ngAggregateText">{{row.label CUSTOM_FILTERS}} {{formatAggregates(row)}}</span><div class="{{row.aggClass()}}"></div></div>',
      rowTemplate = '<div {0}' +
        'ng-style="{ cursor: row.cursor }" ng-repeat="col in renderedColumns" ng-class="col.colIndex()" class="ngCell {{col.cellClass}}">' +
        '<div class="ngVerticalBar" ng-style="{height: rowHeight}" ng-class="{ ngVerticalBarVisible: !$last }">&nbsp;</div><div ng-cell></div></div>',
      cellTemplate = '<div '+ numericFieldAlignment + ' class="ngCellText" ng-class="col.colIndex()">{0}</div>',
      standardFieldCellTemplate = '<a ng-href="{{getDefaultActionUrl(row.entity, col.colDef.fieldDef)}}" title="{0}" data-delay="1000" data-trigger="hover">{{fieldValue(row.entity, col.colDef.fieldDef)}}</a>',
      selectFromFieldCellAddonTemplate = ' <a ng-if="row.entity.{0} && userCanView(col.colDef.fieldDef.selectFrom)" ng-href="{{getFormUrlForReferencedEntity(row.entity, col.colDef.fieldDef)}}">'+
        '<span class="hover-action" title="Επεξεργασία {1}" data-delay="1000" data-trigger="hover"><i class="fa fa-external-link"/></span></a>',
      booleanFieldCellTemplate = '<span class="text-primary" ng-if="row.getProperty(col.field)"><i class="fa fa-check"/></span>',
      htmlFieldCellTemplate = '<span ng-bind-html="fieldValue(row.entity, col.colDef.fieldDef)"></span>',
      checkboxCellTemplate = '<div class=\"ngSelectionCell\"><input tabindex=\"-1\" class=\"ngSelectionCheckbox\" type=\"checkbox\" ng-checked=\"row.selected\" /></div>',
      checkboxHeaderTemplate = '<input class=\"ngSelectionHeader\" type=\"checkbox\" ng-show=\"multiSelect\" ng-model=\"allSelected\" ng-change=\"toggleSelectAll(allSelected, true)\"/>',
      numericHeaderCellTemplate = '<div class="ngHeaderSortColumn {{col.headerClass}}" ng-style="{cursor: col.cursor}" ng-class="{ ngSorted: !noSortVisible }">'+
        '<div ng-click="col.sort($event)" ng-class="\'colt\' + col.index" class="ngHeaderText numeric-field">{{col.displayName}}</div>'+
        '<div class="ngSortButtonDown" ng-show="col.showSortButtonDown()"></div>'+
        '<div class="ngSortButtonUp" ng-show="col.showSortButtonUp()"></div>'+
        '<div class="ngSortPriority">{{col.sortPriority}}</div>'+
        '</div>'+
        '<div ng-show="col.resizable" class="ngHeaderGrip" ng-click="col.gripClick($event)" ng-mousedown="col.gripOnMouseDown($event)"></div>',
        defaultHeaderCellTemplate = numericHeaderCellTemplate.replace('numeric-field', '');

    var rowNgClasses = [];
    if ($scope.entityDefinition.hasWarnings) {
      rowNgClasses.push('warning: entityDefinition.hasWarnings(row.entity)');
    }
    if ($scope.entityDefinition.hasErrors) {
      rowNgClasses.push('danger: entityDefinition.hasErrors(row.entity)');
    }
    var rowNgClass = rowNgClasses.length > 0 ? 'ng-class="{'+rowNgClasses.join(', ')+'}" ' : '';

    // FIXME: This guy does not recognize view properties from other controllers!!!!
    function toLocationSearchOptions($scope, searchOptions) {
      var groups = searchOptions.groups === undefined ? $scope.groups : searchOptions.groups;
      var filterCriteria = searchOptions.filterCriteria === undefined ? $scope.search.criteria : searchOptions.filterCriteria;
      var result = angular.extend({}, {
        size: parseInt(searchOptions.pageSize !== undefined ? searchOptions.pageSize : $scope.pagingOptions.pageSize),
        page: parseInt(searchOptions.currentPage !== undefined ? searchOptions.currentPage : $scope.pagingOptions.currentPage),
        q: translateCodeType(searchOptions.filterText !== undefined ? searchOptions.filterText : $scope.filterOptions.filterText),
        sort: ngGridSortInfoToLocationSortOptions(searchOptions.sortInfo !== undefined ? searchOptions.sortInfo : $scope.sortInfo),
        groups: groups && groups.length > 0 ? groups.join(',') : undefined,
        show: searchOptions.showViews !== undefined ? searchOptions.showViews : $scope.showViews,
        hide: searchOptions.hideViews !== undefined ? searchOptions.hideViews : $scope.hideViews
      }, filterCriteria);
      if (result.size === MIN_PAGE_SIZE || isNaN(result.size)) {
        delete result.size;
      }
      if (result.page === 1 || isNaN(result.page)) {
        delete result.page;
      }
      // in case it is space, undefined, null remove it
      ['q', 'sort', 'show', 'hide'].forEach(function(key) {
        if (!result[key]) {
          delete result[key];
        }
      });
      for(var key in result) {
        if (result.hasOwnProperty(key) && (result[key] === undefined || result[key] === null /* XXX Should remove the null guard if we want to search for null*/)) {
          delete result[key];
        }
      }
//      console.debug('toLocationSearchOptions', result);
      return result;
    }

    function translateCodeType(searchCriteria, reverseTranslation) {
      var field;
      if (!searchCriteria) {
        return searchCriteria;
      }
      for (var i=0; i<$scope.entityDefinition.fields.length; i++) {
        field=$scope.entityDefinition.fields[i];
        if (!field.hasOwnProperty('selectFrom') || !Array.isArray(field.selectFrom) ||
          (field.selectFrom.length === 0 || !field.selectFrom[0].hasOwnProperty('id'))) {
          continue;
        }
        for (var j=0; j<field.selectFrom.length; j++) {
          if (reverseTranslation && field.selectFrom[j].id === searchCriteria) {
            return field.selectFrom[j].text;
          } else if (field.selectFrom[j].text.toLowerCase() === searchCriteria.toLowerCase()) {
            return field.selectFrom[j].id;
          }
        }
      }
      return searchCriteria;
    }

    var uiSortFieldToApiSortFieldMap = {};
    $scope.entityDefinition.fields.forEach(function(fieldDef) {
      if (fieldDef.name && fieldDef.selectFrom && fieldDef.selectFrom.url) {
        if (fieldDef.selectFrom.entityDisplayName) {
          if (fieldDef.selectFrom.entityDisplayNameFields) {
            uiSortFieldToApiSortFieldMap[fieldDef.name] = fieldDef.selectFrom.entityDisplayNameFields.map(function(fieldName) {
              return fieldDef.name+'.'+fieldName;
            });
          } else {
            console.error('Sorting by '+fieldDef.name+' will not work properly because no entityDisplayNameFields attribute has been declared into entityDefinition with url: '+fieldDef.selectFrom.url);
          }
        } else {
          uiSortFieldToApiSortFieldMap[fieldDef.name] = [fieldDef.name+'.name'];
        }
      }
    });

    function uiSearchOptionsToEntityAPISearchOptions(uiSearchOptions) {
      var entityAPISearchOptions = angular.copy(uiSearchOptions);

      if (entityAPISearchOptions.page) {
        entityAPISearchOptions.page = entityAPISearchOptions.page - 1;
      }

      if (entityAPISearchOptions.sort && entityAPISearchOptions.sort.length > 0) {
        if (entityAPISearchOptions.sort && !angular.isArray(entityAPISearchOptions.sort)) {
          entityAPISearchOptions.sort = [entityAPISearchOptions.sort];
        }
//        console.debug('entityAPISearchOptions.sort', entityAPISearchOptions.sort);
        entityAPISearchOptions.sort = entityAPISearchOptions.sort.map(function(uiFieldsList) {
          var apiFields = [];
          uiFieldsList.split(',').forEach(function(uiFieldName) {
            if (!uiSortFieldToApiSortFieldMap[uiFieldName]) {
              apiFields.push(uiFieldName);
            } else {
              Array.prototype.push.apply(apiFields, uiSortFieldToApiSortFieldMap[uiFieldName]);
            }
          });
          return apiFields.join(',');
        });
      }

      return entityAPISearchOptions;
    }

    function getPagedDataAsync(apiAction, args) {
//      console.log('getPagedDataAsync', arguments);
      if (listColumns) {
        args.fields = listColumns;
      }
      $scope.entityList = $scope.entityAPI[apiAction](args);
      $scope.entityList.$promise = $scope.entityList.$promise.then(function(obj) {
        /*
         * Transforms the response adding a page element which contains all page related stuff
         */
        obj.page = {
          size: obj.size,
          totalElements: obj.totalElements,
          totalPages: obj.totalPages,
          number: obj.number
        };
        delete obj.size;
        delete obj.totalElements;
        delete obj.totalPages;
        delete obj.number;
        delete obj.numberOfElements;
        delete obj.lastPage;
        delete obj.firstPage;
        return obj;
      });
      $scope.entityList.$promise = $scope.entityList.$promise.then(apiService.retrocycle);
      $scope.entityList.$promise.then(function(data) {
        if (angular.isArray(data)) {
          $scope.myData = data;
          $scope.totalServerItems = data.length;
        } else {
          $scope.myData = data.content;
          $scope.totalServerItems = data.page.totalElements !== undefined ?
            data.page.totalElements :
            (args.page * args.size + data.content.length +
              (data.content.length < args.size ? 0 : 1))
          ;
        }
        if ($scope.entityDefinition.listView && $scope.entityDefinition.listView.dynamicColumnAttributes) {
          var dynamicColumnAttributes = $scope.entityDefinition.listView.dynamicColumnAttributes(args);
          $scope.dynamicColumnAttributes = dynamicColumnAttributes;
          if (!$scope.gridOptions.$gridScope) {
            return;
          }
          angular.forEach($scope.gridOptions.$gridScope.columns, function(gridColumn) {
            var fieldName = gridColumn.field;
            if (fieldName && dynamicColumnAttributes[fieldName] && dynamicColumnAttributes[fieldName].hasOwnProperty('visible')) {
              gridColumn.visible = dynamicColumnAttributes[fieldName].visible || false;
            }
          });
        }
        if (!$scope.$$phase) {
          $scope.$apply();
        }
      });
    }

    var MIN_PAGE_SIZE = 10;

    function getNormalizedLocationSearchOptions(locationSearchOptions) {
      var result = angular.copy(locationSearchOptions);

      result.size = parseInt(result.size);
      if (isNaN(result.size) || result.size < MIN_PAGE_SIZE) {
        result.size = MIN_PAGE_SIZE;
      }

      result.page = parseInt(result.page);
      if (isNaN(result.page) || result.page < 1) {
        result.page = 1;
      }

//      console.debug('getNormalizedLocationSearchOptions', result);
      return result;
    }

    function filterProps(obj, predicate) {
      if (!obj) {
        return obj;
      }
      var filteredProps = {};
      for(var propName in obj) {
        if (obj.hasOwnProperty(propName) && predicate(propName, obj)) {
          filteredProps[propName] = obj[propName];
        }
      }
      return filteredProps;
    }

    function advancedCriteriaProps(propName) {
      return propName !== 'page' && propName !== 'size' && propName !== 'q' && propName !== 'action' && propName !== 'sort' && !frontEndProps(propName);
    }

    var frontEndPropMap = {show: true, hide: true};
    $scope.registerLocationSearchFrontEndProps = function(extraProps) {
      extraProps.forEach(function(propName) {
        frontEndPropMap[propName] = true;
      });
    };

    function frontEndProps(propName) {
      return frontEndPropMap[propName] || false;
    }

    function backEndProps(propName) {
      return !frontEndProps(propName);
    }

    function areLocationSearchOptionsEqual(o1, o2) {
//        console.debug('areLocationSearchOptionsEqual', 'enter', 'o1', o1, 'o2', o2);
      var result = (function() {
        function propertiesAreEqual(o1, o2) {
          // precondition: o1, o2 are defined
          var key;
          for(key in o1) {
            if (o1.hasOwnProperty(key)) {
              if (!o2.hasOwnProperty(key)) {
                return false;
              }
              // converts values to string before comparing
              if ((((o1[key] || '') + '') || undefined) !== (((o2[key] || '') + '') || undefined)) {
                return false;
              }
            }
          }
          return true;
        }
        if (o1 === undefined && o2 === undefined) {
          return true;
        }
        if (o1 === undefined && Object.keys(o2).length === 0 ||
          o2 === undefined && Object.keys(o1).length === 0) {
          return true;
        }
        if (o1 === undefined || o2 === undefined) {
          return false;
        }
        // we are sure that both o1 && o2 are defined at this point
        if (!propertiesAreEqual(o1, o2)) {
          return false;
        }
        return propertiesAreEqual(o2, o1);
      })();
//        console.debug('areLocationSearchOptionsEqual', 'exit', result);
      return result;
    }

    function search(locationSearchOptions/*, replace*/) {
      function notEmptyProps(propName, obj) {
        return obj[propName] !== undefined && obj[propName] !== null;
      }

//      var previousOptions = previousLocationSearchOptions;
//      previousLocationSearchOptions = $location.search();

//      // XXX Trick to support changing location search options programatically as well as via URL
//      if (!areLocationSearchOptionsEqual(locationSearchOptions, $location.search())) {
//        var newLocation = $location.search(locationSearchOptions);
//        if (replace) {
//          newLocation.replace();
//        }
//        // Returning because we expect a location update event to call this function again
//        return;
//      }

//      // TODO: Move somewhere else !!!!!!
//      setViewsVisibility(locationSearchOptions.show, locationSearchOptions.hide);
//
//      if (areLocationSearchOptionsEqual(filterProps(locationSearchOptions, backEndProps), filterProps(previousOptions, backEndProps)) &&
//          !areLocationSearchOptionsEqual(filterProps(locationSearchOptions, frontEndProps), filterProps(previousOptions, frontEndProps))) {
//        return;
//      }

      var normalizedLocationSearchOpts = getNormalizedLocationSearchOptions(locationSearchOptions);

      $scope.search.term = translateCodeType(normalizedLocationSearchOpts.q || null, true);
      $scope.filterOptions.filterText = $scope.search.term;

      var advancedSearchCriteria = filterProps(normalizedLocationSearchOpts, advancedCriteriaProps);
      var apiAction;

      $scope.search.criteria = advancedSearchCriteria;
      if ($scope.entityDefinition.advancedSearch && $scope.entityDefinition.advancedSearch.apiAction) {
        apiAction = $scope.entityDefinition.advancedSearch.apiAction;
      } else {
        apiAction = 'query';
      }

      if ($scope.entityDefinition.advancedSearch) {
        // TODO calculate a short description for the advanced search criteria
        $scope.search.criteriaDescription = angular.equals(filterProps(advancedSearchCriteria, notEmptyProps), {}) ? '' : 'Με φίλτρο...';
      }

      function getCriteriaDescription(locationSearchOptions) {
        var result = [];
        if (locationSearchOptions.q) {
          result.push(translateCodeType(locationSearchOptions.q || null, true));
        }
        if ($scope.search.criteriaDescription) {
          result.push($scope.search.criteriaDescription);
        }
        return result.join(', ');
      }

      var criteriaDescription = getCriteriaDescription(normalizedLocationSearchOpts);

      $window.document.title = stringUtils.format('{0}{1} - {2}',
        $scope.entityDefinition.name.plural,
        criteriaDescription ? ' (' + criteriaDescription + ')' : '',
        appConstants.appName
      );

      getPagedDataAsync(apiAction, uiSearchOptionsToEntityAPISearchOptions(normalizedLocationSearchOpts));
    }

    var normalizedLocationSearchOptions = getNormalizedLocationSearchOptions($location.search());

    $scope.totalServerItems = 0;

    $scope.filterOptions = {
      filterText: normalizedLocationSearchOptions.q,
      useExternalFilter: true
    };

    $scope.pagingOptions = {
      pageSizes: [MIN_PAGE_SIZE, 50, 100, 1000, 100000],
      pageSize: normalizedLocationSearchOptions.size,
      currentPage: normalizedLocationSearchOptions.page
    };

    /**
     * i.e. sort=country.name,type,ASC&sort=codeIATA,DESC&sort=xxx -> {fields: ['country.name', 'type', 'codeIATA', 'xxx'], directions: ['ASC', 'ASC', 'DESC', 'ASC']}
     */
    function locationSortOptionsToNgGridSortInfo(locationSearchOptions) {
      var sortInfo = {fields: [], directions: [], columns: []};
      if (locationSearchOptions.sort) {
        var sort = angular.isArray(locationSearchOptions.sort) ? locationSearchOptions.sort : [locationSearchOptions.sort];
        sort.forEach(function(sources) {
          /*
           * expects the sources to be a
           * concatenation of Strings delimited by comma. If the last element is 'ASC' or 'DESC' it's
           * considered a Direction otherwise it is considered a simple property
           */
          var props = sources.split(',');
          var dir = 'asc';
          if (props[props.length - 1].match(/^(ASC)|(asc)|(DESC)|(desc)$/)) {
            dir = props[props.length - 1].toLowerCase();
            props = props.slice(0, -1);
          }
          // append fields with props
          sortInfo.fields = sortInfo.fields.concat(props);
          // populate directions
          for(var i=0; i<props.length; i++) {
            sortInfo.directions.push(dir);
          }
        });
      }
      return sortInfo;
    }

    /**
     * Converts a location string to search options object
     * @param locationString
     * @returns {{}}
     */
    function locationStringToSearchOptions(locationString) {
      var searchOptions = {};
      if (locationString.indexOf('?') < 0) {
        return searchOptions;
      }
      var searchString = locationString.substring(locationString.indexOf('?') + 1);
      searchString.split('&').forEach(function(item) {
        var prop = item.split('='), key = prop[0], val = prop[1];
        if (!searchOptions.hasOwnProperty(key)) {
          searchOptions[key] = val;
        } else {
          if (!angular.isArray(searchOptions[key])) {
            searchOptions[key] = [searchOptions[key]];
          }
          searchOptions[key].push(val);
        }
      });
      return searchOptions;
    }

    /**
     * i.e. {fields: ['country.name', 'type', 'codeIATA', 'xxx'], directions: ['asc', 'asc', 'desc', 'asc']} -> sort=country.name,type,ASC&sort=codeIATA,DESC&sort=xxx
     */
    function ngGridSortInfoToLocationSortOptions(sortInfo) {
      var sortOptions = [],
        lastDirection,
        sortOptionsIndex = -1;
      for(var i=0; i<sortInfo.fields.length; i++) {
        if (i === 0 || sortInfo.directions[i] !== lastDirection) {
          if (lastDirection && lastDirection.toLowerCase() !== 'asc') {
            sortOptions[sortOptionsIndex].push(lastDirection);
          }
          sortOptionsIndex ++;
          sortOptions[sortOptionsIndex] = [];
        }
        sortOptions[sortOptionsIndex].push(sortInfo.fields[i]);
        lastDirection = sortInfo.directions[i];
      }
      if (lastDirection && lastDirection.toLowerCase() !== 'asc') {
        sortOptions[sortOptionsIndex].push(lastDirection.toUpperCase());
      }
      if (sortOptions.length === 0) {
        return;
      }
      return sortOptions.map(function(item) {
        return item.join(',');
      });
    }

    $scope.sortInfo = locationSortOptionsToNgGridSortInfo(normalizedLocationSearchOptions);

    $scope.groups = normalizedLocationSearchOptions.groups ? normalizedLocationSearchOptions.groups.split(',') : [];

    $scope.search = {};

    /**
     * Transforms formCriteria to API Action params according to the field type
     */
    function apiActionParams(formCriteria) {
      var params = {};
      angular.forEach(formCriteria, function(value, key) {
        if (value && value.id) {
          params[key] = value.id;
        } else if ($scope.entityDefinition.advancedSearch.fieldsMap[key] &&
          $scope.entityDefinition.advancedSearch.fieldsMap[key].multiValue) {
          angular.forEach(value, function(arrayItem) {
            if (arrayItem.value) {
              params[arrayItem.id] = arrayItem.value;
            } else {
              params[key] = params[key] || [];
              params[key].push(arrayItem.id);
            }
          });
        } else if (angular.isDate(value)) {
          params[key] = $filter('date')(value, 'yyyy-MM-dd');
        } else if (value !== '' && value !== undefined && value !== null) {
          params[key] = value;
        }
      });
      return params;
    }

    /**
     * Transforms UI location search options to criteria form options according to the field type.
     */
    function locationSearchToFormCriteria() {
      var formCriteria,
        locationSearchOpts = $location.search(),
        advancedSearchOptions = filterProps(locationSearchOpts, advancedCriteriaProps);

      // parse a date in yyyy-mm-dd format
      function parseDate(input) {
        if (!input) {
          return null;
        }
        var parts = input.split('-');
        // new Date(year, month [, day [, hours[, minutes[, seconds[, ms]]]]])
        return new Date(parts[0], parts[1]-1, parts[2]); // Note: months are 0-based
      }

      function advancedSearchOptionsToFormCriteria() {
        var formCriteria = {};
        angular.forEach($scope.entityDefinition.advancedSearch.fieldsMap, function (fieldDef, fieldName) {
          var paramValue = advancedSearchOptions[fieldName];
          var mappedOpts = [];
          if (angular.equals(fieldDef.type, {dateTime: 'dd/mm/yyyy'})) {
            formCriteria[fieldName] = parseDate(paramValue);
          } else if (fieldDef.selectFrom && fieldDef.multiValue && angular.isArray(fieldDef.selectFrom) &&
            fieldDef.selectFrom.length > 0 && fieldDef.selectFrom[0].hasOwnProperty('value')) {
            fieldDef.selectFrom.forEach(function (selectFromOption) {
              if (advancedSearchOptions[selectFromOption.id] !== undefined) {
                mappedOpts.push(selectFromOption);
              }
            });
            formCriteria[fieldName] = mappedOpts;
          } else if (fieldDef.selectFrom && !fieldDef.multiValue &&
              (angular.isArray(fieldDef.selectFrom.data || fieldDef.selectFrom) ||
                  angular.isFunction(fieldDef.selectFrom))) {
            var selectFrom = angular.isFunction(fieldDef.selectFrom) ? fieldDef.selectFrom() :  (fieldDef.selectFrom.data || fieldDef.selectFrom);
            selectFrom.forEach(function (selectFromOption) {
              if (advancedSearchOptions[fieldName] === selectFromOption.id) {
                formCriteria[fieldName] = selectFromOption;
              }
            });
          } else if (fieldDef.selectFrom && fieldDef.multiValue && angular.isArray(fieldDef.selectFrom) &&
              fieldDef.selectFrom.length > 0) {
            fieldDef.selectFrom.forEach(function (selectFromOption) {
              if (advancedSearchOptions[fieldName] && advancedSearchOptions[fieldName].indexOf(selectFromOption.id) >= 0) {
                mappedOpts.push(selectFromOption);
              }
            });
            formCriteria[fieldName] = mappedOpts;
          } else if (fieldDef.selectFrom && angular.isObject(fieldDef.selectFrom)) {
            if (advancedSearchOptions[fieldName]) {
              var fieldValue;
              if (fieldDef.multiValue && angular.isArray(advancedSearchOptions[fieldName])) {
                fieldValue = [];
                angular.forEach(advancedSearchOptions[fieldName], function(item){
                  fieldValue.push(fieldDef.selectFrom.resourceApi.get({identifier: item}));
                });
              } else {
                fieldValue = fieldDef.selectFrom.resourceApi.get({identifier: advancedSearchOptions[fieldName]});
              }
              formCriteria[fieldName] = fieldValue;
            }
          } else if (advancedSearchOptions[fieldName] !== undefined) {
            formCriteria[fieldName] = advancedSearchOptions[fieldName];
          }
        });
//        console.debug('locationSearchToFormCriteria retrieved', formCriteria);
        return formCriteria;
      }

      var lastCriteria = criteriaStore.retrieve($scope.entityDefinition);
      var lastApiCriteria = $scope.entityDefinition.advancedSearch ? apiActionParams(lastCriteria) : {};
      var areEqual = angular.equals(lastApiCriteria, advancedSearchOptions);

      if (areEqual) {
        formCriteria = lastCriteria;
      } else if ($scope.entityDefinition.advancedSearch) {
        formCriteria = advancedSearchOptionsToFormCriteria();
        // cache criteria
        criteriaStore.persist($scope.entityDefinition, formCriteria);
      }
      return formCriteria;
    }


    var criteriaStore = (function criteriaStore() {
      if (!$rootScope.lastCriteria) {
        $rootScope.lastCriteria = {};
      }
      var storage = $rootScope.lastCriteria;
      function key(entityDef) {
        return entityDef;
      }
      return {
        persist: function(entityDef, criteriaObj) {
          storage[key(entityDef)] = criteriaObj;
        },
        retrieve: function(entityDef) {
          return storage[key(entityDef)];
        }
      };
    })();

    $scope.$watch('pagingOptions', function (newVal, oldVal) {
      if (newVal !== oldVal) {
        if (newVal && oldVal && parseInt(newVal.pageSize) !== parseInt(oldVal.pageSize)) {
          // calculate new page from current row and oldValue
          var startRow = Math.min((oldVal.currentPage - 1) * oldVal.pageSize + 1, $scope.totalServerItems);
          var pageNo = Math.floor(startRow / newVal.pageSize);
          newVal.currentPage = pageNo + 1;
        }
        var locationSearchOptions = toLocationSearchOptions($scope, {pageSize: newVal.pageSize, currentPage: newVal.currentPage});
        updateLocationSearchAndSearch(locationSearchOptions);
      }
    }, true);

    $scope.$watch('sortInfo', function (newVal, oldVal) {
      var oldSortOption = ngGridSortInfoToLocationSortOptions(oldVal);
      var newSortOption = ngGridSortInfoToLocationSortOptions(newVal);
      if (!angular.equals(newSortOption, oldSortOption)) {

        var locationSearchOptions = toLocationSearchOptions($scope, {sortInfo: newVal});
        updateLocationSearchAndSearch(locationSearchOptions);
      }
    }, true);

    $scope.filter = function(filterText) {
      var locationSearchOptions = toLocationSearchOptions($scope, {currentPage: 1, filterText: filterText});
      updateLocationSearchAndSearch(locationSearchOptions);
    };

    $scope.$on('$locationChangeSuccess', function(event, newLocationString, oldLocationString) {
      if (!areLocationSearchOptionsEqual(
        filterProps(locationStringToSearchOptions(oldLocationString), backEndProps),
        filterProps(locationStringToSearchOptions(newLocationString), backEndProps))) {
        search($location.search());
        fillScopeCriteriaFromLocationSearch();
      }
    });

    $scope.fieldValue = util.fieldValueFn(EntityDefinitions, $sce);

    $scope.getFormUrlForReferencedEntity = util.getFormUrlForReferencedEntity;

    $scope.getDefaultActionUrl = function(entity, fieldDefinition) {
      if (!entity) {
        return '';
      }
      var url = $scope.entityDefinition.listView && $scope.entityDefinition.listView.defaultActionUrl ?
        $scope.entityDefinition.listView.defaultActionUrl(entity, fieldDefinition, angular.copy($location.search())) :
        $scope.entityDefinition.url +'/'+entity.id+'/edit';
      return '#/'+url;
    };

    $scope.getActionUrl = util.getActionUrlFn($scope);

    /**
     * Expects search.criteria to be on scope
     */
    function runAdvancedSearch() {
      criteriaStore.persist($scope.entityDefinition, $scope.criteria);
      var locationSearchOptions = toLocationSearchOptions($scope, {currentPage: 1, filterText: undefined, filterCriteria: apiActionParams($scope.criteria)});
      updateLocationSearchAndSearch(locationSearchOptions);
    }

    function updateLocationSearchAndSearch(searchOptions) {
      var backEndLocationSearchOptions = filterProps(searchOptions, backEndProps);
      var updatedLocationSearchOptions = angular.extend(
          backEndLocationSearchOptions,
          filterProps($location.search(), frontEndProps)
        );
      if (areLocationSearchOptionsEqual($location.search(), updatedLocationSearchOptions)) {
        // backend location search options have not changed but user requested data refresh
        search(updatedLocationSearchOptions);
      } else {
        $location.search(updatedLocationSearchOptions);
      }
    }

    function clearCriteria() {
      $scope.criteria = {};
    }

    function clearCriteriaGroup(group) {
      if (!$scope.entityDefinition || !$scope.entityDefinition.advancedSearch) {
        return;
      }
      var fields = $scope.entityDefinition.advancedSearch.fields;
      for (var i = 0; i < fields.length; i++) {
        if (fields[i].group === group) {
          delete $scope.criteria[fields[i].name];
        }
      }
    }

    function fillScopeCriteriaFromLocationSearch() {
      $scope.criteria = locationSearchToFormCriteria() || {};
      $scope.select2 = select2Fn($scope.criteria, $scope, 'criteriaForm', apiService.retrocycle);
    }

    $scope.clearCriteria = function() {
      clearCriteria();
    };

    $scope.clearCriteriaGroup = function(group) {
      clearCriteriaGroup(group);
    };

    $scope.runAdvancedSearch = function() {
      runAdvancedSearch();
    };

    $scope.openAdvancedSearchDialog = function() {
      fillScopeCriteriaFromLocationSearch();

      $scope.closeAndSearch = function() {
        $scope.$dismissModal();
        runAdvancedSearch();
      };

      $scope.clear = function() {
        clearCriteria();
      };

      $scope.close = function() {
        $scope.$dismissModal();
      };

      dialogsService.showModal({
        templateUrl: $scope.entityDefinition.advancedSearch.templateUrl,
        scope: $scope
      });
    };

    $scope.importFile = function() {
      $scope.methodOptions = [{id: 'POST', text: 'Δημιουργία εγγραφών'}, {id: 'PUT', text: 'Aντικατάσταση εγγραφών'}, {id: 'PATCH', text: 'Ενημέρωση εγγραφών'}];
      var utf8EncodingOption = {id: 'UTF-8', text: 'UTF-8'};
      $scope.encodingOptions = [{id: 'Windows-1253', text: 'Windows-1253'}, utf8EncodingOption, {id: 'UTF-16', text: 'UTF-16'}];
      var jsonFileTypeOption = {id: 'json', text: 'Αρχείο JSON', mediaType: 'application/json'};
      $scope.fileTypeOptions = [
        {id: 'csvWithComma', text: 'Τιμές διαχωρισμένες με κόμμα (,)', mediaType: 'text/csv'},
        {id: 'csvWithSemicolon', text: 'Τιμές διαχωρισμένες με ελληνικό ερωτηματικό (;)', mediaType: 'text/csv;delimiter=0x3b'},
        {id: 'csvWithTab', text: 'Τιμές διαχωρισμένες με χαρακτήρα στηλοθέτη (tab)', mediaType: 'text/csv;delimiter=0x09'},
        jsonFileTypeOption
      ];

      $scope.importFileOptions = $scope.importFileOptions || {
        method: $scope.methodOptions[0],
        encoding: $scope.encodingOptions[0],
        fileType: $scope.fileTypeOptions[1]
      };

      $scope.impSelect2 = select2Fn($scope.importFileOptions, $scope, 'importFileForm', apiService.retrocycle);

      $scope.triggerFileOpen = function() {
        angular.element('#impfile').click();
      };

      $scope.close = function() {
        $scope.$dismissModal();
      };

      $scope.downloadFileWithErrors = function() {
        saveAs(createXlsFile($scope.errorData),
          $scope.selectedFiles[0].name+' - Σφάλματα εισαγωγής '+$scope.entityDefinition.name.pluralGeneral+'.xlsx');
      };

      $scope.onFileSelect = function($files) {
        var filteredFiles = [],
          i, f;
        for ( i = 0; i<$files.length; i++) {
          f = $files[i];
          filteredFiles.push(f);
        }
        if (filteredFiles.length > 0) {
          f = filteredFiles[filteredFiles.length - 1];
          if (f.type === 'application/json') {
            $scope.importFileOptions.fileType = jsonFileTypeOption;
            $scope.importFileOptions.encoding = utf8EncodingOption;
          }
        }
        $scope.selectedFiles = filteredFiles;
        $scope.successfulMessage = null;
        $scope.errorMessage = null;
        $scope.errorData = null;

        $scope.progress = 0;
        $scope.showProgressBar = false;
      };

      $scope.upload = function() {
        if (!$scope.selectedFiles || $scope.selectedFiles.length === 0) {
          $window.alert('Πρέπει να επιλέξετε ένα αρχείο που να περιέχει '+$scope.entityDefinition.name.plural);
          return;
        }
        $scope.uploading = true;
        $scope.uploadStarttime = new Date().getTime();
        $scope.successfulMessage = null;
        $scope.errorMessage = null;
        $scope.errorData = null;

        $scope.progress = 0;
        $scope.showProgressBar = true;

        var successFn = function(/*response*/) {
//          console.log('Successful upload');
          $scope.showProgressBar = false;
          $scope.successfulMessage = $scope.selectedFiles[0].name;
        };

        var errorFn = function(response) {
//          console.error('Error during upload');
          $scope.showProgressBar = false;
          var data = response.data,
            status = response.status;
          if (400 === status){
            $scope.errorMessage = data.reason || 'Μη έγκυρο αρχείο εισαγωγής';
            $scope.errorData = data;
          } else {
            console.error('error: ', data, 'status:', status);
            if (status === 409) {
              $scope.errorMessage = data.reason || 'Δεν μπορεί να γίνει η ενέργεια διότι θα παραβίαζε την ακεραιότητα της βάσης δεδομένων';
            } else {
              $scope.errorMessage = data.reason || 'Συνέβη ένα σφάλμα κατά την επικοινωνία με τον διακομιστή. Δοκιμάστε αργότερα κι αν το πρόβλημα παραμένει, επικοινωνήστε με την τεχνική υπηρεσία';
            }
          }
        };

        var reader = new FileReader();
        var progressPromise;

        reader.onload = function(e) {
          var MAX_T = 60000,
            MAX = 85,
            REFRESH_MILLIS = 1000;
          function progressFn() {
            var now = new Date().getTime(),
              t = now - $scope.uploadStarttime;
            // The progress is a sigmoid function
            // see http://www.wolframalpha.com/input/?i=-42.5+%2B+85+*+1+%2F+%281+%2B+e%5E-x%29+from+-2+to+4
            $scope.progress = 10 + MAX * 1 / (1 + Math.exp(-(t)/(MAX_T/4)));
//            console.debug('progress', $scope.progress);
            progressPromise = $timeout(progressFn, REFRESH_MILLIS, true);
          }
          progressFn();
          performHttpRequest(
            $scope.importFileOptions.method.id,
            $scope.entityDefinition.url,
            e.target.result,
            $scope.importFileOptions.encoding.id,
            $scope.importFileOptions.fileType.mediaType,
            successFn,
            errorFn
          );
        };

        function performHttpRequest(method, resource, contents, encoding, mediaType, successCallback, errorCallback) {
//          console.debug('HTTP Request', method, resource, encoding);
          $http({
            method: 'POST',
            url: apiService.apiPathname + resource + '/bulk',
            params: {
              _method: method.toUpperCase()
            },
            headers: {
              'Content-Type': mediaType+';charset='+encoding
            },
            data: contents
          }).
            then(successCallback, errorCallback).
            finally(function() {
              $timeout.cancel(progressPromise);
              $scope.uploading = false;
              $scope.progress = 100;
            });
        }

        reader.readAsText($scope.selectedFiles[0], $scope.importFileOptions.encoding.id);

      };

      dialogsService.showModal({
        templateUrl: entityListImportTemplateUrl,
        scope: $scope
      });
    };

    function transformEntitiesToBulkExportRequest(entities) {
      function derez(value, replaceRefs) {

        // The derez recurses through the object, producing the deep copy where referenced entities are replaced with ids.

        var i,        // The loop counter
          name,       // Property name
          target;     // The new object or array

        // typeof null === 'object', so go on if this value is really an object but not
        // one of the weird builtin objects.

        if (typeof value === 'object' && value !== null &&
          !(value instanceof Boolean) &&
          !(value instanceof Date)    &&
          !(value instanceof Number)  &&
          !(value instanceof RegExp)  &&
          !(value instanceof String)) {

          // If it is an array, replicate the array.

          if (Object.prototype.toString.apply(value) === '[object Array]') {
            target = [];
            for (i = 0; i < value.length; i += 1) {
              target[i] = derez(value[i], replaceRefs);
            }
          } else {
            // if the object has an id property, replace with a reference
            if (replaceRefs && Object.prototype.hasOwnProperty.call(value, 'id')) {
              target = '/' + value.id;
            } else {
              // replicate the object.
              target = {};
              for (name in value) {
                if (Object.prototype.hasOwnProperty.call(value, name)) {
                  target[name] = derez(value[name], true);
                }
              }
            }
          }

          return target;
        }
        return value;
      }
      return derez(entities);
    }

    $scope.exportJson = function() {
      var itemsToExport = $scope.gridOptions.selectedItems.length === 0 ? $scope.gridOptions.data : $scope.gridOptions.selectedItems;
      var output = {
        content: transformEntitiesToBulkExportRequest(itemsToExport)
      };
      saveAs(new Blob([JSON.stringify(output)], {type: 'application/json;charset=utf-8'}), $scope.entityDefinition.name.plural+'.json');
    };

    $scope.exportCsv = function() {
      /*jshint eqnull:true */
      function ngGridCsvExport(scope, gridOptions, opts) {
        opts = opts || {};
        function csvStringify(str) {
          if (str == null) { // we want to catch anything null-ish, hence just == not ===
            return '';
          }
          if (typeof(str) === 'number') {
            return '' + str;
          }
          if (typeof(str) === 'boolean') {
            return (str ? 'TRUE' : 'FALSE') ;
          }
          if (typeof(str) === 'string') {
            return str.replace(/"/g,'""');
          }

          return JSON.stringify(str).replace(/"/g,'""');
        }

        var keys = [];
        var csvData = '';
        for (var f in gridOptions.$gridScope.columns) {
          if (gridOptions.$gridScope.columns.hasOwnProperty(f) &&
            gridOptions.$gridScope.columns[f].visible &&
            gridOptions.$gridScope.columns[f].field !== '\u2714')
          {
            keys.push(gridOptions.$gridScope.columns[f].field);
            csvData += '"' ;
            if(typeof gridOptions.$gridScope.columns[f].displayName !== 'undefined'){
              csvData += csvStringify(gridOptions.$gridScope.columns[f].displayName);
            }
            else{
              csvData += csvStringify(gridOptions.$gridScope.columns[f].field);
            }
            csvData +=  '";';
          }
        }

        function swapLastCommaForNewline(str) {
          var newStr = str.substr(0,str.length - 1);
          return newStr + '\n';
        }

        csvData = swapLastCommaForNewline(csvData);
        var gridData = scope[gridOptions.data];
        for (var gridRow in gridData) {
          var rowData = '';
          for ( var k in keys) {
            var curCellRaw;

            if (opts != null && opts.columnOverrides != null && opts.columnOverrides[keys[k]] != null) {
              curCellRaw = opts.columnOverrides[keys[k]](
                $utilityService.evalProperty(gridData[gridRow], keys[k]));
            } else {
              curCellRaw = $utilityService.evalProperty(gridData[gridRow], keys[k]);
            }

            rowData += '"' + csvStringify(curCellRaw) + '";';
          }
          csvData += swapLastCommaForNewline(rowData);
        }
        return csvData;
      }
      /*jshint eqnull:false */

      var csvData = ngGridCsvExport($scope, $scope.gridOptions);
      // XXX \ufeff results to creating a BOM in the output file which makes Excel understand UTF-8 encoding of CSV
      saveAs(new Blob(['\ufeff', csvData], {type: 'text/csv;charset=utf-8'}), $scope.entityDefinition.name.plural+'.csv');
    };

    var entityListFieldsFn = util.entityViewFieldsFn('listView');
    var entityListFieldsMap = {};

    // Configure the columns of ngGrid
    var gridColumnDefs = [];
    entityListFieldsFn($scope.entityDefinition).forEach(function(fieldDef) {
      // Skip column if showInList === false. Default is true. 'optional' means do not show initially but allow user to show it
      if (fieldDef.showInList === false) {
        return;
      }

      if (!fieldDef.name) {
        console.warn('Found field without a "name" property in entity definition "'+$scope.entityDefinition.name.singular+'". Anonymous field is', fieldDef);
      }

      var cellContentTemplate = '{{row.getProperty(col.field)}}';
      var headerCellTemplate = (fieldDef.type && fieldDef.type.numeric) ? numericHeaderCellTemplate : defaultHeaderCellTemplate;
      if (!fieldDef.type || (fieldDef.type !== 'boolean' && fieldDef.type !== 'html')) {
        cellContentTemplate = stringUtils.format(standardFieldCellTemplate,
          $scope.entityDefinition.readOnly ? 'Επισκόπηση ' : 'Επεξεργασία '+$scope.entityDefinition.name.singularGeneral);

        if (!fieldDef.multiValue && fieldDef.selectFrom && fieldDef.selectFrom.url) {
          cellContentTemplate = cellContentTemplate +
            stringUtils.format(selectFromFieldCellAddonTemplate, fieldDef.name, fieldDef.selectFrom.name.singularGeneral);
        }
      } else if (fieldDef.type === 'boolean') {
        cellContentTemplate = booleanFieldCellTemplate;
      } else if (fieldDef.type === 'html') {
        cellContentTemplate = htmlFieldCellTemplate;
      }

      entityListFieldsMap[fieldDef.name] = fieldDef;

      gridColumnDefs.push({
        field: fieldDef.name,
        displayName: fieldDef.label,
        width: (fieldDef.width ? stringUtils.repeat('*', Math.ceil(fieldDef.width*4)) : '**'),
        fieldDef: fieldDef,
        cellTemplate: stringUtils.format(cellTemplate, cellContentTemplate),
        visible: (fieldDef.showInList === undefined || fieldDef.showInList === true),
        sortable: (fieldDef.name && !fieldDef.formula ? true : false),
        headerCellTemplate: headerCellTemplate
      });
    });

    var listColumns = (function() {
      function buildListColumns(columnDefs) {
        function onlyUnique(value, index, self) {
          return self.indexOf(value) === index;
        }
        function OptimizationException(message, param) {
          this.message = message;
          this.param = param;
        }
        var columns = [];
        columnDefs.forEach(function(columnDef) {
          var fieldDef = columnDef.fieldDef;
          if (fieldDef.formula) {
            if (fieldDef.formulaFields) {
              columns = columns.concat(fieldDef.formulaFields);
            } else {
              throw new OptimizationException('Query optimization cannot be performed because property "formulaFields" is missing in field definition', fieldDef);
            }
          } else if (!fieldDef.name) {
            throw new OptimizationException('Query optimization cannot be performed because property "name" is missing in field definition', fieldDef);
          } else if (fieldDef.selectFrom && angular.isObject(fieldDef.selectFrom) && !angular.isArray(fieldDef.selectFrom)) {
            var referencedEntityDef = fieldDef.selectFrom;
            if (referencedEntityDef.entityDisplayName) {
              if (referencedEntityDef.entityDisplayNameFields) {
                columns = columns.concat(referencedEntityDef.entityDisplayNameFields.map(function(entityDisplayNameField) {
                  return fieldDef.name + '.' + entityDisplayNameField;
                }));
              } else {
                console.warn('Query optimization is not complete because there is a missing property entityDisplayNameFields in entity definition', referencedEntityDef);
                columns.push(fieldDef.name);
              }
            } else {
              columns.push(fieldDef.name + '.' + 'name');
            }
          } else {
            columns.push(fieldDef.name);
          }
        });
        columns.push('id');
        columns = columns.filter(onlyUnique);
        return columns.join(',');
      }
      try {
        return buildListColumns(gridColumnDefs);
      } catch (e) {
        console.error(e.message, e.param);
      }
    })();

    // XXX Work around a bug in ng-grid 2.0.11#ngRowFactory.getGrouping() `val = val ? val.toString() : 'null';`
    // which does not allow us to group by complex object fields (i.e. selectFrom)
    $utilityService.evalProperty = util.fieldValueFn(EntityDefinitions, $sce, entityListFieldsMap);

    /**
     * TODO Document
     */
    function calculateAggregates(row, aggregatesSpec, fieldsMap) {
      var aggregates = angular.copy(aggregatesSpec);
      angular.forEach(aggregatesSpec, function(fieldAggregates, fieldName) {
        if (fieldAggregates.sum !== undefined) {
          aggregates[fieldName].sum = sum(row, fieldName, fieldsMap[fieldName]);
        }
        // TODO Support more aggregate functions
      });
      return aggregates;
    }

    var aggregateFunctionLabel = {
      sum: '',
      average: 'Μ/Ο',
      count: '#',
      min: 'min',
      max: 'max'
    };

    function sum(row, fieldName, fieldDefinition) {
      var sumAggFieldName = fieldName+'.sum';
      var recurse = function (cur) {
        if (cur.isAggRow) {
          if (!cur.hasOwnProperty(sumAggFieldName)) {
            var subtotal = 0,
              childRows = cur.aggChildren.length > 0 ? cur.aggChildren : cur.children;
            angular.forEach(childRows, function(childRow) {
              subtotal += (recurse(childRow) || 0);
            });
            cur[sumAggFieldName] = subtotal;
          }
          return cur[sumAggFieldName];
        } else {
          return parseFloat(fieldDefinition.formula ?
            fieldDefinition.formula(cur.entity) :
            util.getFieldPathValue(cur.entity, fieldName));
        }
      };
      return recurse(row);
    }

    var aggFieldsMap = angular.extend({}, $scope.entityDefinition.fieldsMap, entityListFieldsMap);

    $scope.formatAggregates = function(row) {
      var aggregates = calculateAggregates(row, $scope.entityDefinition.defaultAggregates, aggFieldsMap);
      var labels = [];
      angular.forEach(aggregates, function(fieldAggregates, fieldName) {
        var aggregateLabels = [];
        angular.forEach(fieldAggregates, function(aggregateValue, aggregateFunctionName) {
          aggregateLabels.push(aggregateFunctionLabel[aggregateFunctionName] + ': ' + $filter('number')(aggregateValue, aggregateValue % 1 === 0 ? 0 : 2)); // TODO $filter should be based on field type
        });
        labels.push(aggFieldsMap[fieldName].label +
          ' ' + aggregateLabels.join(', '));
      });
      return '(' + row.totalChildren() + ') '+labels.join(' | ');
    };

    // ----------------------------------------
    // Handle column visibility (Section start)
    // ----------------------------------------
    function getViewsVisibility(gridColumns) {
      var views = {fieldsToShow: [], fieldsToHide: []};
      angular.forEach(gridColumns, function(column) {
        if (!column.colDef.fieldDef || !column.colDef.fieldDef.name) {
          return;
        }
        if ($scope.entityDefinition.listView && $scope.entityDefinition.listView.dynamicColumnAttributes) {
          var dynamicColumnAttributes = $scope.dynamicColumnAttributes || $scope.entityDefinition.listView.dynamicColumnAttributes({});
          if (dynamicColumnAttributes[column.colDef.fieldDef.name] &&
            !dynamicColumnAttributes[column.colDef.fieldDef.name].hasOwnProperty('visible')) {
            return;
          }
        }
        if (column.visible !== (column.colDef.fieldDef.showInList !== 'optional')) {
          if (column.visible) {
            views.fieldsToShow.push(column.colDef.fieldDef.name);
          } else {
            views.fieldsToHide.push(column.colDef.fieldDef.name);
          }
        }
      });
      return views;
    }

    function replaceFieldsOfGroupWithGroupName(views) {
      var groups = getFieldGroups();
      angular.forEach(groups, function(group) {
        if (containsAllFieldsOfGroup(views.fieldsToShow, group)) {
          views.fieldsToShow = views.fieldsToShow.filter(function(item) {
            return group.fields.indexOf(item) < 0;
          });
          views.fieldsToShow.push(group.name);
        }
        if (containsAllFieldsOfGroup(views.fieldsToHide, group)) {
          views.fieldsToHide = views.fieldsToHide.filter(function(item) {
            return group.fields.indexOf(item) < 0;
          });
          views.fieldsToHide.push(group.name);
        }
      });
    }

    function getFieldGroups() {
      var groups = {};
      angular.forEach($scope.entityDefinition.fields, function(field) {
        if (field.hasOwnProperty('fieldGroup')) {
          if (!groups.hasOwnProperty(field.fieldGroup)) {
            groups[field.fieldGroup] = {name: field.fieldGroup, fields: []};
          }
          groups[field.fieldGroup].fields.push(field.name);
        }
      });
      return groups;
    }

    function containsAllFieldsOfGroup(fieldsToShowOrHide, group) {
      var copyOfFieldsOfGroup = group.fields.slice(0);
      var fieldIdx;
      angular.forEach(fieldsToShowOrHide, function(field) {
        fieldIdx = copyOfFieldsOfGroup.indexOf(field);
        if (fieldIdx >= 0) {
          copyOfFieldsOfGroup.splice(fieldIdx, 1);
        }
      });
      return copyOfFieldsOfGroup.length === 0;
    }

    $scope.isViewVisible = function(groupName) {
      if (!$scope.gridOptions || !$scope.gridOptions.$gridScope) {
        return false;
      }
      var visibleViews = $scope.gridOptions.$gridScope.columns.filter(function(gridColumn) {
        return gridColumn.visible;
      }).map(function(gridColumn) {
            return gridColumn.field;
          });
      var groups = getFieldGroups();
      return containsAllFieldsOfGroup(visibleViews, groups[groupName]);
    };

    $scope.toggleViewVisibility = function(groupName) {
      var isCurrentlyVisible = $scope.isViewVisible((groupName));
      angular.forEach($scope.gridOptions.$gridScope.columns, function(gridColumn) {
        if ((gridColumn.field === groupName) ||
            (gridColumn.colDef.fieldDef && gridColumn.colDef.fieldDef.fieldGroup === groupName)) {
          gridColumn.visible = !isCurrentlyVisible;
        }
      });
    };

    $scope.$watch('gridOptions.$gridScope.columns', function (newColumns, oldColumns) {
      if (!newColumns || !oldColumns || newColumns.length !== oldColumns.length) {
        return;
      }
      var views = getViewsVisibility(newColumns);
      replaceFieldsOfGroupWithGroupName(views);

      var showViews = views.fieldsToShow.join();
      var hideViews = views.fieldsToHide.join();
      if (showViews !== $scope.showViews || hideViews !== $scope.hideViews) {
        var oldLocationSearchOptions = $location.search();
        if ((oldLocationSearchOptions.show || '') !== (showViews || '')) {
          $location.search('show', showViews).replace();
        }
        if ((oldLocationSearchOptions.hide || '') !== (hideViews || '')) {
          $location.search('hide', hideViews).replace();
        }
      }

    }, true);

    $scope.$on('$locationChangeSuccess', function(event, newLocationString/*, oldLocationString*/) {
      function setViewsVisibility(viewsToShow, viewsToHide) {
        $scope.showViews = viewsToShow;
        $scope.hideViews = viewsToHide;

        if (!$scope.gridOptions || (!$scope.gridOptions.$gridScope && !gridColumnDefs)) {
          return;
        }
        var newViewsVisibility = (viewsToShow || '').split(',').reduce(function(accumulator, item) {
          accumulator[item] = true;
          return accumulator;
        }, {});
        newViewsVisibility = (viewsToHide || '').split(',').reduce(function(accumulator, item) {
          accumulator[item] = false;
          return accumulator;
        }, newViewsVisibility);

        var columns = $scope.gridOptions.$gridScope ? $scope.gridOptions.$gridScope.columns : gridColumnDefs;
        angular.forEach(columns, function(gridColumn) {
          var fieldDef = gridColumn.fieldDef || gridColumn.colDef.fieldDef;
          if (newViewsVisibility.hasOwnProperty(gridColumn.field)) {
            gridColumn.visible = newViewsVisibility[gridColumn.field];
          } else if (fieldDef && newViewsVisibility.hasOwnProperty(fieldDef.fieldGroup)) {
            gridColumn.visible = newViewsVisibility[fieldDef.fieldGroup];
          } else if (fieldDef) {
            gridColumn.visible = (fieldDef.showInList === undefined || fieldDef.showInList === true);
          }
        });
      }
      var searchOptions = locationStringToSearchOptions(newLocationString);
      setViewsVisibility(searchOptions.show, searchOptions.hide);
    });
    // ----------------------------------------
    // Handle column visibility (Section end)
    // ----------------------------------------

    $scope.gridOptions = {
      plugins: [],
      primaryKey: 'id',
      data: 'myData',
      selectedItems: [],
      enablePaging: true,
      showFooter: true ,
      enableColumnResize: true,
      enableColumnReordering: true,
      showSelectionCheckbox: true,
      selectWithCheckboxOnly: true,
      enableHighlighting: false,
      showColumnMenu: true,
      showGroupPanel: true,
      i18n: 'el',
      totalServerItems: 'totalServerItems',
      pagingOptions: $scope.pagingOptions,
      filterOptions: $scope.filterOptions,
      useExternalSorting: true,
      sortInfo: $scope.sortInfo,
      groups: $scope.groups,
      columnDefs: gridColumnDefs,
      rowTemplate: stringUtils.format(rowTemplate, rowNgClass),
      aggregateTemplate: aggregateTemplate,
      checkboxCellTemplate: checkboxCellTemplate,
      checkboxHeaderTemplate: checkboxHeaderTemplate
    };

    // Execute search after all controllers have finished loading...
    $scope.$evalAsync(function() {
      if (Object.keys($location.search()).length === 0 && $scope.entityDefinition.advancedSearch && $scope.entityDefinition.advancedSearch.getDefaultCriteria) {
        $scope.criteria = $scope.entityDefinition.advancedSearch.getDefaultCriteria();
        runAdvancedSearch();
      } else {
        search($location.search());
      }
      if ($scope.fillCriteriaOnInit) {
        fillScopeCriteriaFromLocationSearch();
      }
    });

  }]);

  module.filter('entitySelectFromFilter', ['EntityDefinitions', 'select2Service', function(EntityDefinitions, select2Service) {
    // TODO optimize using cache
    return function(input, entityUrl) {
      if (!entityUrl) {
        return input;
      }
      var mapFn = select2Service.entityLookupTextFn(
        EntityDefinitions.getEntityDefinitionFromUrl(entityUrl));

      return angular.isArray(input) ? input.map(mapFn).join(', ') : mapFn(input);
    };
  }]);

  module.filter( 'fileSize', function () {
    var units = [
      'bytes',
      'KB',
      'MB',
      'GB',
      'TB',
      'PB'
    ];

    return function( bytes, precision ) {
      if ( isNaN( parseFloat( bytes )) || ! isFinite( bytes ) ) {
        return '?';
      }

      var unit = 0;

      while ( bytes >= 1024 ) {
        bytes /= 1024;
        unit ++;
      }

      return bytes.toFixed( + precision ) + ' ' + units[ unit ];
    };
  });

  return module;
});