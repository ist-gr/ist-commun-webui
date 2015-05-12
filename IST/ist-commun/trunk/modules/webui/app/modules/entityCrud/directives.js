/**
 * Created by antonis on 31/1/14.
 */
/*globals define*/
define(['angular',
  'text!./entityAuditInfo.html',
  'text!./entityFormErrors.html',
  'text!./entityFormEntityFieldInput.html',
  'text!./selectFromEntityFieldAddon.html',
  'angular-busy/angular-busy'
  ], function(angular, entityAuditInfoTemplate, entityFormErrorsTemplate, entityFormEntityFieldInputTemplate, selectFromEntityFieldAddonTemplate) {
  'use strict';

  var module = angular.module('entityCrud.directives', ['cgBusy']);

  module.value('cgBusyDefaults', {
    message:'Περιμένετε...',
    backdrop: true,
    delay: 200,
    minDuration: 120
  });

  module.directive('seAuditInfo', function() {
    return {
      restrict: 'E',
      replace: true, // the template will replace the current element.
      template: entityAuditInfoTemplate
    };
  });

  // TODO
  module.directive('seInput', function() {
    return {
      restrict: 'E',
      replace: true,
      template: entityFormEntityFieldInputTemplate,
      scope: {
        fieldDef: '=field',
        name: '=',
        bar: '=ngModel'
      },
      link: function(/*scope*/) {

      }
    };
  });

  module.directive('seSelectAddon', function() {
    return {
      restrict: 'E',
      replace: true,
      template: selectFromEntityFieldAddonTemplate,
      scope: {
        fieldDef: '=field',
        extraClass: '@extraClass'
      },
      link: function(scope) {
        // XXX TODO Find a more DRY and loosely coupled way to get needed dependencies for template
        scope.userCanView = scope.$parent.userCanView;
        scope.getFormUrlForReferencedEntity = scope.$parent.getFormUrlForReferencedEntity;
        scope.isViewable = scope.$parent.isViewable;
        scope.entity = scope.$parent.entity;
        scope.getEntityFieldLeafParentScope = scope.$parent.getEntityFieldLeafParentScope;
        scope.getEntityFieldLeafName = scope.$parent.getEntityFieldLeafName;
      }
    };
  });

  module.directive('seFormErrors', function() {
    return {
      restrict: 'E',
      replace: true, // the template will replace the current element.
      template: entityFormErrorsTemplate
    };
  });

  module.directive('bsDatepickerBindFix', [
    function () {
      return {
        require: '?ngModel', // get a hold of NgModelController
        restrict: 'A', // only activate on element attribute
        priority: 1,//scope: { ngModel: '=' },
        link: function (scope, element, attrs, ngModel) {
          if (!ngModel) {
            return;
          }

          // Specify how UI should be updated
          ngModel.$render = function() {
            var val2render = ngModel.$viewValue || '',
              elementVal = element.val();
            element.val(val2render);
            if(val2render!==elementVal) {
              element.datepicker('update', val2render);
            }

          };
        }
      };
    }
  ]);

  module.directive('seDatetime', ['dateFilter',
    function (dateFilter) {
      return {
        require: '?ngModel', // get a hold of NgModelController
        restrict: 'A', // only activate on element attribute
        link: function (scope, element, attrs, ngModel) {
          if (!ngModel) {
            return;
          }
          /*XXX: Hacky Workaroud in order to fix bug:
           Όταν σε dateRange αλλάζουμε τιμή στο ένα part μπορεί να
           αλλάξει η τιμή στο απέναντι part και να μην το πάρουμε χαμπάρι.
           The bug is in the bootstrap-datepicker.js. See also:
           http://eternicode.github.io/bootstrap-datepicker/
            */
          element.on('keydown', function(evt) {
            if (attrs.bsDatepickerBindFix === '') {
              var keycode = evt.charCode || evt.keyCode;
              //catch delete and backspace keys and delete the whole date
              if (keycode  === 8 || keycode === 46) {
                element.val(null);
                element.trigger('input');
              //do not catch tabs
              } else if (keycode === 9) {
                return true;
              //catch every other keypress and override it
              } else {
                return false;
              }
            }
          });
          var dateMatcher;

          if (attrs.dateFormat === 'dd/MM/yy' || attrs.dateFormat === 'dd/MM/yyyy') {
            dateMatcher = /^([1-9]|(?:[0-3][0-9]))[\/\.\-]?([0-1][0-9])?[\/\.\-]?([0-9]{2}(?:[0-9]{2})?)?$/;
          } else {
            dateMatcher = /^(?:([1-9]|(?:[0-3][0-9]))[\/\.\-]?([0-1][0-9])?[\/\.\-]?([0-9]{2}(?:[0-9]{2})?)?[ ])?([0-9][0-9]):?([0-9][0-9])$/;
          }

          // Using AngularJS built in date filter, convert our date to RFC 3339
          ngModel.$formatters = [function (value) {
            return value && angular.isNumber(value) ? dateFilter(value, attrs.dateFormat) : '';
          }];

          // Listen for change events to enable binding
          element.on('blur', function() {
            if (!ngModel.$modelValue) {
              return;
            }
            var formattedDate = dateFilter(ngModel.$modelValue, attrs.dateFormat);
            if (ngModel.$viewValue !== formattedDate) {
              element.val(formattedDate);
            }
          });

          // Convert the string value to Date object.
          ngModel.$parsers = [function (value) {
            if (!value || !angular.isString(value)) {
              return null;
            }
            var now = new Date(),
              invalidDate = new Date('invalid'),
              result = invalidDate;
            if (dateMatcher.test(value)) {
              var groups = dateMatcher.exec(value);
              var year = groups.length < 4 || groups[3] === undefined ? now.getFullYear() : ( parseInt(groups[3]) + 0 < 100 ? parseInt(groups[3]) + (parseInt(groups[3] < 50 ? 2000 : 1900) ) : groups[3] ),
                  month = groups.length < 3 || groups[2] === undefined ? now.getMonth() : groups[2] - 1,
                day = groups.length < 2 || groups[1] === undefined ? now.getDate() : groups[1];

              if (groups.length === 4) {
                result = new Date(year,
                  month,
                  day,
                  attrs.dateCeiling ? 23 : 0,
                  attrs.dateCeiling ? 59 : 0,
                  attrs.dateCeiling ? 59 : 0,
                  attrs.dateCeiling ? 999 : 0
                );
              } else if (groups[4] && groups[5]) {
                result = new Date(year,
                  month,
                  day,
                  groups[4],
                  groups[5],
                  attrs.dateCeiling ? 59 : 0,
                  attrs.dateCeiling ? 999 : 0
                );
              }
//                console.log('date', result, groups);
            }
            ngModel.$setValidity('seDatetime', result !== invalidDate);
            return (result === invalidDate) ? null : result.valueOf();

          }];
        }
      };
    }]);

  module.directive('seNumeric', ['NumericFieldDefinition','NumberFormatter',
    function (NumericFieldDefinition, NumberFormatter) {
      return {
        require: '?ngModel', // get a hold of NgModelController
        restrict: 'A', // only activate on element attribute
        link: function (scope, element, attrs, ngModel) {
          if (!ngModel) {
            return;
          }

          var patternType = JSON.parse(attrs.numberFormat);
          attrs.ngPattern = NumericFieldDefinition.numericFieldPatterns[patternType.numeric].pattern;
          var message = NumericFieldDefinition.numericFieldPatterns[patternType.numeric].patternMessage;
          var error = 'entityForm["' + attrs.name + '"].$error';
          scope.$watch(error, function(newValue) {
            if (newValue.pattern) {
              element.before('<i class="fa fa-warning form-control-feedback" rel="tooltip" title="'+message+'"></i>');
            } else {
              element.prev().remove();
            }
          }, true);

          element.on('blur', function() {
            if (!ngModel.$modelValue) {
              return;
            }
            var formattedValue = NumberFormatter.format(ngModel.$modelValue);
            if (ngModel.$viewValue !== formattedValue) {
              element.val(formattedValue);
            }
          });

          // Convert the string value to number.
          ngModel.$parsers = [function (value) {
            if (value !== undefined && value !== null && value !== '') {
              var comma = value.indexOf(',');
              var dot = value.indexOf('.');
              var commaCount = (value.match(/,/g) || []).length;
              var dotCount = (value.match(/\./g) || []).length;
              var numValue;

              if (comma !== -1 && dot !== -1) {//if comma and dot are present see which one is first and replace with dot or empty string accordingly
                if (comma < dot) {
                  numValue = value.replace(/,/g, '');
                } else {
                  numValue = value.replace(/\./g, '').replace(',', '.');
                }
              } else if (comma !== -1 && dot === -1 && commaCount > 1) {//if only comma is present and there are more than one, replace with empty string => the user is using it a thousands separator
                numValue = value.replace(/,/g, '');
              } else if (comma !== -1 && dot === -1 && commaCount === 1) {//if only comma is present and there is one, replace with dot => the user is using it a decimal separator
                numValue = value.replace(',', '.');
              } else if (dot !== -1 && comma === -1 && dotCount === 1) {//if only one dot is present, do nothing => user is using it as decimal separator.
                numValue = value;
              } else if (dot !== -1 && comma === -1 && dotCount > 1) {//if more than one dot is present, replace it with empty string => user is using it as thousands separator.
                numValue = value.replace(/\./g, '');
              } else {//no dot or comma is present, so it's an integer
                numValue = value;
              }
              //If parsed value contains a dot it means we have a float, else it's an integer
              if (numValue.indexOf('.') !== -1) {
                return parseFloat(numValue);
              } else {
                return parseInt(numValue);
              }
            }
            return null;
          }];

          ngModel.$formatters = [function (value) {
            return NumberFormatter.format(value);
          }];
        }
      };
    }]);

  // Workaround for bug #1404
  // https://github.com/angular/angular.js/issues/1404
  // Source: http://plnkr.co/edit/hSMzWC?p=preview
  module.config(['$provide', function($provide) {
    $provide.decorator('ngModelDirective', function($delegate) {
      var ngModel = $delegate[0], controller = ngModel.controller;
      ngModel.controller = ['$scope', '$element', '$attrs', '$injector', function(scope, element, attrs, $injector) {
        var $interpolate = $injector.get('$interpolate');
        attrs.$set('name', $interpolate(attrs.name || '')(scope));
        $injector.invoke(controller, this, {
          '$scope': scope,
          '$element': element,
          '$attrs': attrs
        });
      }];
      return $delegate;
    });
    $provide.decorator('formDirective', function($delegate) {
      var form = $delegate[0], controller = form.controller;
      form.controller = ['$scope', '$element', '$attrs', '$injector', function(scope, element, attrs, $injector) {
        var $interpolate = $injector.get('$interpolate');
        attrs.$set('name', $interpolate(attrs.name || attrs.ngForm || '')(scope));
        $injector.invoke(controller, this, {
          '$scope': scope,
          '$element': element,
          '$attrs': attrs
        });
      }];
      return $delegate;
    });
  }]);

  /*
   Given a numeric value it will format it with dots as thousands separator and comma as decimal separator
   */
  module.factory('NumberFormatter', function() {
    return {
      format: function(numVal) {
        if (numVal === undefined || numVal === null || numVal === '') {
          return;
        }
        var x = numVal.toString().split('.');
        var x1 = x[0];
        var x2 = x.length > 1 ? ',' + x[1] : '';
        var rgx = /(\d+)(\d{3})/;
        while (rgx.test(x1)) {
          x1 = x1.replace(rgx, '$1' + '.' + '$2');
        }
        return x1 + x2;
      }
    };
  });

  module.factory('NumericFieldDefinition', function() {
    return {
      numericFieldPatterns: {
        TWO_DECIMALS_NUMBER: {pattern: '/^-?(\\d*\\.\\d{1,2}|\\d+)$/', patternMessage: 'Πρέπει να είναι της μορφής (-)9999,99'},
        SIX_DECIMALS_NUMBER: {pattern: '/^-?(\\d*\\.\\d{1,6}|\\d+)$/', patternMessage: 'Πρέπει να είναι της μορφής (-)9999,999999'},
        MONETARY_AMOUNT: {pattern: '/^(\\d*\\.\\d{1,2}|\\d+)$/', patternMessage: 'Πρέπει να είναι της μορφής 9999,99'},
        MONETARY_AMOUNT_4DECS: {pattern: '/^(\\d*\\.\\d{1,4}|\\d+)$/', patternMessage: 'Πρέπει να είναι της μορφής 9999,9999'},
        NON_NEGATIVE_INTEGER: {pattern: '/^[0-9]*$/', patternMessage: 'Πρέπει να είναι μη αρνητικός ακέραιος αριθμός'},
        NATURAL_NUMBER: {pattern: '/^[1-9][0-9]*$/', patternMessage: 'Πρέπει να είναι ακέραιος αριθμός μεγαλύτερος του 0'}
      }
    };
  });

  return module;
});