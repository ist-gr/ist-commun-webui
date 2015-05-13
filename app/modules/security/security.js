/*global define, App, Login */
define(['angular',
        '../entityCrud/entityCrud',
        'text!./login.html',
        'text!./403.html',
        'text!./entityRevisionForm.html',
        'text!./entityRevisionCriteria.html',
        'appConstants',
        'jquery',
        'angular-route',
        'angular-base64',
        'ng-infinite-scroll',
        '../core',
        './http-auth-interceptor',
        'css!de-metr/login-minimized',
        'css!de-metr/coming-soon-minimized'
        ],function(angular, entityCrud, loginTemplate, unauthorizedTemplate, entityRevisionFormTemplate, entityRevisionCriteriaTemplate, appConstants, $) {
  
  'use strict';
  
  var LOGIN_TEMPLATE_URL = 'modules/security/login.html';
  var UNAUTHORIZED_TEMPLATE_URL = 'modules/security/403.html';
  var ENTITY_REVISION_CRITERIA_TEMPLATE_URL = 'modules/security/entityRevisionCriteria.html';

  // XXX The authentication logic is a quick n'dirty solution based on jhipster's authentication but without need for java sessions
  // TODO Refactor it to be simpler and beautiful

  var module = angular.module('security', ['ngRoute', 'ngResource', 'base64', 'core', 'http-auth-interceptor','dialogs','infinite-scroll', 'entityCrud']);
  angular.module('infinite-scroll').value('THROTTLE_MILLISECONDS', 250);

  module.run(['$templateCache', function($templateCache) {
    $templateCache.put(LOGIN_TEMPLATE_URL, loginTemplate);
    $templateCache.put(UNAUTHORIZED_TEMPLATE_URL, unauthorizedTemplate);
    $templateCache.put(ENTITY_REVISION_CRITERIA_TEMPLATE_URL, entityRevisionCriteriaTemplate);
  }]);
  
  module.config(['$routeProvider', function ($routeProvider) {
    $routeProvider
      .when('/login', {
        title: 'Είσοδος - '+appConstants.appName,
        templateUrl: LOGIN_TEMPLATE_URL,
        controller: 'LoginCtrl',
        isUnprotected: true
      })
      .when('/unauthorized', {
        templateUrl: UNAUTHORIZED_TEMPLATE_URL
      });
  }]);

  module.directive('scProtect', ['$compile', function($compile) {
    return {
      compile: function(tElement, tAttrs) {
        var src = tAttrs.scProtect;
        if (!src) {
          throw 'Protect expression not specified in sc-protect directive';
        }
        // Clone and then clear the template element to prevent expressions from being evaluated
        var $clone = tElement.clone();
        tElement.html('');
        // Loop through $clone's children of type input and set ngReadonly attribute to the src expression
        $clone.find(':input').each(function () {
          if ($(this).attr('type') === 'checkbox') {
            $(this).attr('ng-disabled', src);
          } else {
            $(this).attr('ng-readonly', src);
          }
        });

        return function ($scope, $element) {
          $element.html($clone.html());
          $compile($element.contents())($scope);
        };
      }
    };
  }]);

  module.factory('userService', ['$http', '$base64', 'apiService', 'authService', function ($http, $base64, apiService, authService) {
    function errorHandler(callback) {
      return function(data, status, headers, config) {
        user.isLoggedIn = false;
        console.error('http get error', 'data', data, 'status', status, 'headers', JSON.stringify(headers), 'config', JSON.stringify(config));
        callback.error({status: status, reason: data.reason});
      };
    }
    function getUserDetails(callback, invokeOnlyIfNotLoggedIn) {
      if (invokeOnlyIfNotLoggedIn && user.username) {
        callback.success();
        return;
      }
      $http.get(apiService.apiPathname + 'security/userDetails', {
        headers: {'Accept': 'application/json'},
        withCredentials: true, // See https://developer.mozilla.org/en-US/docs/HTTP/Access_control_CORS?redirectlocale=en-US&redirectslug=HTTP_access_control#section_5
        timeout: 5000
      }).success(function (data/*, status, headers, config*/) {
          user.isLoggedIn = true;
          user.username = data.username;
          user.profile = {
            id: data.id,
            fullName: data.fullName,
            firstName: data.firstName,
            lastName: data.lastName,
            avatar: data.avatar,
            airports: data.airports
          };
          var authorities = {};
          angular.forEach(data.authorities, function(role) {
            if (!role.authority) {
              console.warn('Unexpected blank authority received from server: '+role.authority);
            } else if (authorities[role.authority]) {
              console.warn('Unexpected duplicate authority received from server: '+role.authority+', '+authorities[role.authority]);
            } else {
              authorities[role.authority] = true;
            }
          });
          if (!angular.equals(user.authorities, authorities)) {
            user.authorities = authorities;
          }
          callback.success();
        }).error(errorHandler(callback));
    }
    var user = {
      isLoggedIn: false,
      profile: {},
      authorities: {},
      authenticate: function(callback) {
        getUserDetails(callback, true);
        return;
      },
      login: function login(username, password, rememberMe, callback) {
        var data = 'username=' + username + '&password=' + password + '&_spring_security_remember_me=' + rememberMe + '&submit=Login';
        $http.post(apiService.apiRootPath + '/login', data, {
          headers: {
            'Content-Type': 'application/x-www-form-urlencoded'
          },
          ignoreAuthModule: 'ignoreAuthModule' // TODO remove if HTTP auth interceptor is not used
        }).success(function (/*data, status, headers, config*/) {
            user.isLoggedIn = true;
            if (!rememberMe) {
              var authHeader = $base64.encode(username+':'+password);
              $http.defaults.headers.common.Authorization = 'Basic '+authHeader;
              // XXX: password needed for HTTP GET request directly from browser (see statistics/dashboard.html) if no session or rememberMe cookie is used
              user.password = password;
            } else {
              delete user.password;
            }
            user.username = username;
            getUserDetails(callback);
          }).error(errorHandler(callback));
      },
      logout: function logout() {
        //delete $http.defaults.headers.common.Authorization;
        $http.get(apiService.apiRootPath + '/logout')
          .success(function (/*data, status, headers, config*/) {
            user.isLoggedIn = false;
            user.profile = {};
            authService.loginCancelled(); // TODO remove if HTTP auth interceptor is not used
          });
      }
    };
    user.hasRole = function(roleName) {
      return (user.authorities && (user.authorities[roleName] || user.authorities.ADMIN));
    };
    user.hasAnyRole = function(roleNames) {
      for (var i in roleNames) {
        if (user.hasRole(roleNames[i])) {
          return true;
        }
      }
      return false;
    };
    user.canPerformAction = function(apiUrl, action) {
      var result;
      if ((action === 'create' || action === 'update') && user.hasRole(apiUrl + '_editor')) {
        result = true;
      } else if (action === 'delete' && user.hasRole(apiUrl + '_admin')) {
        result = true;
      } else if (action === 'view' && user.hasRole(apiUrl + '_viewer')) {
        result = true;
      } else {
        result = false;
      }
      return result;
    };
    user.canEdit = function(entityDefinition/*, fieldDef*/) {
      // TODO support field level security later
      if (entityDefinition.isUserProfile){
        return true;
      }
      return user.hasRole(entityDefinition.url + '_editor');
    };
    user.canView = function(entityDefinition/*, fieldDef*/) {
      // TODO support field level security later
      return user.hasRole(entityDefinition.url + '_viewer');
    };

    return user;
  }]);

  module.run(['$rootScope', '$location', 'userService', '$http', '$route', function($rootScope, $location, userService, $http, $route) {
    // adding X-Requested-With HTTP header to avoid browser prompt for credentials
    $http.defaults.headers.common['X-Requested-With'] = 'XMLHttpRequest';

    $rootScope.hasRole = userService.hasRole;
    $rootScope.canPerformAction = userService.canPerformAction;
    $rootScope.userCanEdit = userService.canEdit;
    $rootScope.userCanView = userService.canView;
    $rootScope.hasAnyRole = userService.hasAnyRole;

    $rootScope.$on('$routeChangeStart', function(event, next/*, current*/) {
      if (next.isUnprotected) {
        return;
      }
      // Check if the status of the user. Is it authenticated or not?
      userService.authenticate({
        success: function() {
          // If the login page has been requested and the user is already logged in
          // the user is redirected to the home page
          if ($location.path() === '/login') {
            $location.url('/').replace();
          }
        },
        error: function() {
          $rootScope.$broadcast('event:auth-loginRequired');
        }
      });
    });

    $rootScope.$on('event:unauthorized', function(/*rejection*/) {
      $location.url('/unauthorized').replace();
    });

    // Call when the 401 response is returned by the client
    $rootScope.$on('event:auth-loginRequired', function(/*rejection*/) {
      if ($rootScope.changedUsername !== undefined || $rootScope.changedPassword !== undefined) {
        var rememberMe = ((($http.defaults || {}).headers || {}).common || {}).Authorization === undefined;
        userService.login($rootScope.changedUsername, $rootScope.changedPassword, rememberMe, {
          success: function() {
            console.log('Success');
            delete $rootScope.changedUsername;
            delete $rootScope.changedPassword;
            $route.reload();
          },
          error: function(error) {
            console.log('error: ' + error);
            delete $rootScope.changedUsername;
            delete $rootScope.changedPassword;
            $location.url('/login').replace();
          }
        });
      } else {
        userService.isLoggedIn = false;
        $location.url('/login').replace();
      }
    });
    // Call when the user logs out
    $rootScope.$on('event:auth-loginCancelled', function() {
      userService.profile = {};
      userService.isLoggedIn = false;
      $location.url('/login').replace();
    });
  }]);

  module.controller('LoginCtrl',
      ['$scope', 'userService', '$location',
        function ($scope, userService, $location) {
          $scope.appConstants = appConstants;
          $scope.rememberMe = true;
          $scope.login = function() {
            delete $scope.errorMessage;
            $scope.gotErrorResponse = false;
            var $ = angular.element;
            $scope.previousUsername = $scope.username;
            $scope.previousPassword = $scope.password;
            userService.login($scope.username, $scope.password, $scope.rememberMe, {
              success: function() {
                $location.url('/').replace();
              },
              error: function(error) {
                $scope.gotErrorResponse = true;
                console.error(error);
                var errorMessage = 'Τεχνικό πρόβλημα. Ξαναδοκιμάστε σε λίγο και αν συνεχίζεται ζητήστε τεχνική υποστήριξη.';
                if (error.status === 401 && error.reason) {
                  errorMessage = error.reason;
                }
                $scope.errorMessage = errorMessage;
                $('.alert').show(); // TODO Avoid DOM manipulation
              }
            });
          };
        }
      ]);

  module.controller('UserProfileCtrl',
    ['$scope', 'userService', '$location',
      function ($scope, userService, $location) {
        // TODO: Refactor to avoid the 2 lines below
        App.init(); // init layout and core plugins
        Login.init();
        $scope.rememberMe = true;
        $scope.login = function() {
          delete $scope.errorMessage;
          $scope.gotErrorResponse = false;
          var $ = angular.element;
          $scope.previousUsername = $scope.username;
          $scope.previousPassword = $scope.password;
          userService.login($scope.username, $scope.password, $scope.rememberMe, {
            success: function() {
              $location.url('/').replace();
            },
            error: function(error) {
              $scope.gotErrorResponse = true;
              console.error(error);
              var errorMessage = 'Τεχνικό πρόβλημα. Ξαναδοκιμάστε σε λίγο και αν συνεχίζεται ζητήστε τεχνική υποστήριξη.';
              if (error.status === 401 && error.reason) {
                errorMessage = error.reason;
              }
              $scope.errorMessage = errorMessage;
              $('.alert').show(); // TODO Avoid DOM manipulation
            }
          });
        };
      }
    ]);

  module.factory('AuthorityDefinition', ['AuthorityAPI', function(AuthorityAPI) {
    var roleDef = {
      domainClassName: 'gr.com.ist.commun.core.domain.security.AuthorityRole',
      url: 'authorities',
      readOnly: true,
      isValueObject: true, /*A rare case, it is not an entity*/
      resourceApi: AuthorityAPI,
      entityDisplayName: function(authority) { return authority.localizedAuthority; },
      name: {
        singular: 'Δικαιοδοσία',
        singularGeneral: 'δικαιοδοσίας',
        singularAccusative: 'δικαιοδοσία',
        plural: 'Δικαιοδοσίες',
        pluralGeneral: 'δικαιδοσιών'
      },
      fields: [
        {name: 'authority', label: 'Όνομα', length: 50, width: 2}
      ]
    };
    roleDef.fields.push({name: 'includedAuthorities', label: 'Δικαιοδοσίες', width: 2, optional: true, showInList: false, selectFrom: roleDef, multiValue: true});
    return roleDef;
  }]);

  module.factory('RoleGroupDefinition', ['RoleGroupAPI', 'AuthorityDefinition', function(RoleGroupAPI, AuthorityDefinition) {
    var roleDef = {
      domainClassName: 'gr.com.ist.commun.core.domain.security.RoleGroup',
      url: 'roleGroups',
      resourceApi: RoleGroupAPI,
      name: {
        singular: 'Ρόλος',
        singularGeneral: 'ρόλου',
        singularAccusative: 'ρόλο',
        plural: 'Ρόλοι',
        pluralGeneral: 'ρόλων',
        pluralAccusative: 'ρόλους'
      },
      fields: [
        {name: 'name', label: 'Όνομα', length: 50, width: 2},
        {name: 'includedAuthorities', label: 'Δικαιοδοσίες', width: 2, optional: true, showInList: false, selectFrom: AuthorityDefinition, multiValue: true}
      ]
    };
    roleDef.fields.push({name: 'includedRoleGroups', label: 'Ρόλοι', width: 2, optional: true, showInList: false, selectFrom: roleDef, multiValue: true});
    return roleDef;
  }]);

  //Declaring functions to be used both in User and UserProfiled EntityDefinitions
  var validator = function(entity) {
    if (entity.password !== entity.passwordConfirmation) {
      return [{fieldName: 'password', errorMessage: 'Το συνθηματικό και η επιβεβαίωση αυτού δεν ταιριάζουν'}];
    }
  };

  var avatarsList = ['gd', 'man13', 'man20', 'man24', 'man27', 'man2', 'man7',
    'woman10', 'woman23', 'woman3', 'woman7', 'man10', 'man14', 'man23', 'man26', 'man28', 'man5', 'man8', 'woman13', 'woman27', 'woman6',
    'woman9'];

  // Show images on avatars selection
  var avatarFormatter = function avatarFormatter(avatar) {
    return '<img alt src="images/avatars/'+avatar.text+'.png" width="30" height="30"/>';
  };

  var populatePasswordPolicyInfo = function(PasswordPolicyDefinition, scope) {
    PasswordPolicyDefinition.resourceApi.query(function(data){
      for (var i = 0; i < data.content.length; i++) {
        if (data.content[i].isActive) {
          scope.passwordMinChars = data.content[i].minimumLength;
          scope.passwordMaxChars = data.content[i].maximumLength;
          scope.passwordMinCap = data.content[i].minNumberOfCapitalCase ? data.content[i].minNumberOfCapitalCase : 0;
          scope.passwordMinLow = data.content[i].minNumberOfLowerCase ? data.content[i].minNumberOfLowerCase : 0;
          scope.passwordMinNum = data.content[i].minNumberOfDigits ? data.content[i].minNumberOfDigits : 0;
          scope.passwordMinSpecial = data.content[i].minNumberOfSpecials ? data.content[i].minNumberOfSpecials : 0;
        }
      }
    });
  };

  module.factory('UserDefinition', ['UserAPI', '$rootScope', 'RoleGroupDefinition', '$http', 'apiService', 'PasswordPolicyDefinition', function(UserAPI, $rootScope, RoleGroupDefinition, $http, apiService, PasswordPolicyDefinition) {
    return {
      domainClassName: 'gr.com.ist.commun.core.domain.security.User',
      url: 'users',
      resourceApi: UserAPI,
      entityDisplayName: function(user) {
        return user.fullName;
      },
      name: {
        singular: 'Χρήστης',
        singularGeneral: 'χρήστη',
        singularAccusative: 'Χρήστη',
        plural: 'Χρήστες',
        pluralGeneral: 'χρηστών'
      },
      ngController: function(scope) {
        function fullName(names) {
          return (names.fn || '') + ' ' + (names.ln || '');
        }
        scope.$watch('{fn: entity.firstName, ln: entity.lastName}',
          function(newValue, oldValue) {
            if ( newValue !== oldValue ) {
              if (!scope.entity.fullName || fullName(oldValue) === scope.entity.fullName) {
                scope.entity.fullName = fullName(newValue);
              }
            }
          },
          true
        );
        // TODO: Find a better way to avoid showing the login form when password is changed
        scope.$watch('{username: entity.username, password: entity.password}',
          function(newValue/*, oldValue*/) {
            $rootScope.changedUsername = newValue.username;
            $rootScope.changedPassword = newValue.password;
          },
          true
        );
        populatePasswordPolicyInfo(PasswordPolicyDefinition, scope);
      },
      fields: [
        {name: 'username', label: 'Όνομα Χρήστη', length: 60, width: 1},
        {name: 'password', label: 'Συνθηματικό', length: 20, width: 1, type: {passwd: {confirmationField: 'passwordConfirmation'}}, showInList: false, optional: function(entity, isNew) {return !isNew; }},
        {name: 'passwordConfirmation', label: 'Επιβεβαίωση Συνθηματικού', length: 20, width: 1, type: {passwd: {passwordField: 'password'}}, showInList: false, optional: function(entity, isNew) {return !isNew; }},
        {name: 'firstName', label: 'Όνομα', length: 30, width: 1},
        {name: 'lastName', label: 'Επώνυμο', length: 30, width: 1},
        {name: 'fullName', label: 'Ονοματεπώνυμο', length: 60, width: 2},
        {name: 'passwordExpiresOn', label: 'Ημ/νία Λήξης Συνθηματικού', length: 60, width: 1, optional: true, type:{dateTime: 'dd/MM/yyyy HH:mm'}},
        {name: 'disabled', label: 'Απενεργοποιημένος', type: 'boolean', width: 1, optional: true},
        {name: 'locked', label: 'Κλειδωμένος', type: 'boolean', width: 1, optional: true},
        {name: 'email', label: 'Email', length: 50, width: 1, optional: true},
        {name: 'avatar', label: 'Avatar', length: 50, width: 1, optional: true, selectFrom: {data: avatarsList, formatResult: avatarFormatter, formatSelection: avatarFormatter}, showInList: false},
        {name: 'roles', label: 'Ρόλοι Χρήστη', width: 2, optional: true, showInList: false, selectFrom: RoleGroupDefinition, multiValue: true}
      ],
      validator: validator
    };
  }]);

  module.factory('UserProfileDefinition', ['UserProfileAPI', '$rootScope', '$http', 'apiService', 'PasswordPolicyDefinition', 'userService', function(UserProfileAPI, $rootScope, $http, apiService, PasswordPolicyDefinition, userService) {
    return {
      domainClassName: 'gr.com.ist.commun.core.domain.security.UserProfile',
      url: '/security/userProfile',
      resourceApi: UserProfileAPI,
      name: {
        singular: 'Προφίλ χρήστη',
        singularGeneral: 'προφίλ χρήστη',
        plural: 'Προφίλ χρήστη',
        pluralGeneral: 'προφίλ χρήστη'
      },
      ngController: function(scope) {
        function fullName(names) {
          return (names.fn || '') + ' ' + (names.ln || '');
        }
        scope.$watch('{fn: entity.firstName, ln: entity.lastName}',
          function(newValue, oldValue) {
            if ( newValue !== oldValue ) {
              if (!scope.entity.fullName || fullName(oldValue) === scope.entity.fullName) {
                scope.entity.fullName = fullName(newValue);
              }
            }
          },
          true
        );
        // TODO: Find a better way to avoid showing the login form when password is changed
        scope.$watch('{username: entity.username, password: entity.password}',
          function(newValue/*, oldValue*/) {
            $rootScope.changedUsername = newValue.username;
            $rootScope.changedPassword = newValue.password;
          },
          true
        );
        populatePasswordPolicyInfo(PasswordPolicyDefinition, scope);
      },
      fields: [
        {name: 'username', label: 'Όνομα Χρήστη', length: 60, width: 1},
        {name: 'password', label: 'Συνθηματικό', length: 20, width: 1, type: {passwd: {confirmationField: 'passwordConfirmation'}}, optional: true},
        {name: 'passwordConfirmation', label: 'Επιβεβαίωση Συνθηματικού', length: 20, width: 1, type: {passwd: {passwordField: 'password'}}, optional: true},
        {name: 'firstName', label: 'Όνομα', length: 30, width: 1, optional: true},
        {name: 'lastName', label: 'Επώνυμο', length: 30, width: 1, optional: true},
        {name: 'fullName', label: 'Ονοματεπώνυμο', length: 60, width: 2, optional: true},
        {name: 'email', label: 'Email', length: 50, width: 1, optional: true},
        {name: 'avatar', label: 'Avatar', length: 50, width: 1, optional: true, selectFrom: {data: avatarsList, formatResult: avatarFormatter, formatSelection: avatarFormatter}, showInList: false}
      ],
      events: {
        onPostUpdate: function savePreferredAirport(entity, callback) {
          userService.profile.avatar = entity.avatar ? entity.avatar.id : null;
          userService.profile.fullName = entity.fullName;
          userService.profile.firstName = entity.firstName;
          userService.profile.lastName = entity.lastName;
          if (callback) {
            callback(entity);
          }
        }
      },
      validator: validator,
      isUserProfile: true

    };
  }]);


  function transformRequestFn(data) {
    delete data.passwordConfirmation;
    delete data.$cgBusyFulfilled;
    delete data.createdBy;
    delete data.lastModifiedBy;
    return JSON.stringify(data);
  }

  var userOptions = {
    resourceApi: {
      get: {
        transformResponse: function(data) {
          var user = JSON.parse(data);
          user.passwordConfirmation = user.password;
          return user;
        }
      },
      update: {
        transformRequest: transformRequestFn
      },
      create: {
        transformRequest: transformRequestFn
      }
    }
  };

  var userProfileOptions = {
    resourceApi: {
      get: {
        method: 'GET',
        url: 'security/userProfile',
        transformResponse: function(data) {
          var userProfile = JSON.parse(data);
          userProfile.passwordConfirmation = userProfile.password;
          return userProfile;
        }
      },
      update: {
        url: 'security/userProfile?_method=PUT',
        transformRequest: transformRequestFn
      }
    },
    urlPath: 'userProfile',
    noListView: true
  };

  module.factory('PasswordPolicyDefinition', ['PasswordPolicyAPI', function(PasswordPolicyAPI) {
    return {
      domainClassName: 'gr.com.ist.commun.core.domain.security.PasswordPolicy',
      url: 'passwordPolicies',
      resourceApi: PasswordPolicyAPI,
      name: {
        singular: 'Πολιτική συνθηματικού',
        singularGeneral: 'πολιτικής συνθηματικού',
        plural: 'Πολιτικές συνθηματικού',
        pluralGeneral: 'πολιτικών συνθηματικού'
      },
      fields: [
        {name: 'minimumLength', label: 'Ελάχιστος αριθμός χαρακτήρων', length: 5, width: 2},
        {name: 'maximumLength', label: 'Μέγιστος αριθμός χαρακτήρων', length: 5, width: 2},
        {name: 'minNumberOfDigits', label: 'Ελάχιστος αριθμός αριθμητικών χαρακτήρων', width: 2, optional: true},
        {name: 'minNumberOfLowerCase', label: 'Ελάχιστος αριθμός μικρών χαρακτήρων', width: 2, optional: true},
        {name: 'minNumberOfCapitalCase', label: 'Ελάχιστος αριθμός κεφαλαίων χαρακτήρων', width: 2, optional: true},
        {name: 'minNumberOfSpecials', label: 'Ελάχιστος αριθμός ειδικών χαρακτήρων', width: 2, optional: true},
        {name: 'isActive', label: 'Ενεργή', selectFrom: ['true', 'false'], width: 2, optional: true}
      ]
    };
  }]);

  module.factory('EntityRevisionDefinition', ['EntityRevisionAPI', 'dateFilter','UserDefinition', 'EntityDefinitions', function(EntityRevisionAPI, dateFilter, UserDefinition, EntityDefinitions) {
    function entityChangesSummary(entityChanges) {
      /*function propertyChangesSummary(propertyChanges) {
        return 'Properties modified: '+propertyChanges.length;
      }
      function getRevisionType(revisionType) {
        if(revisionType==='MOD'){
          return 'Modified';
        } else if (revisionType==='ADD') {
          return 'Added';
        } else {
          return 'Deleted';
        }
      }
      */
      var entitiesChanged = [];
      entityChanges.forEach(function(change) {
        var entDef = EntityDefinitions.getEntityDefinitionForDomainClass(change);
        if (!entDef) {
          return;
        }
        entitiesChanged.push(EntityDefinitions.getEntityDefinitionForDomainClass(change).name.singular);
      });
      return entitiesChanged.join();
    }
    var def = {
      readOnly: true,
      domainClassName: 'gr.com.ist.commun.core.domain.security.audit.entity.AppEntityRevision',
      url: 'entityRevisions',
      resourceApi: EntityRevisionAPI,
      name: {
        singular: 'Ιστορικό ενεργειών Χρηστών',
        singularGeneral: 'ιστορικού ενεργειών χρηστών',
        plural: 'Ιστορικό ενεργειών Χρηστών',
        pluralGeneral: 'Ιστορικού ενεργειών Χρηστών'
      },
      advancedSearch: {
        templateUrl: ENTITY_REVISION_CRITERIA_TEMPLATE_URL,
        apiAction: 'query',
        fieldsMap: {
          who: {selectFrom: UserDefinition},
          dateFrom: {label: 'Ημερομηνία από', type: {dateTime: 'dd/mm/yyyy'}},
          dateTo: {label: 'Ημερομηνία έως', type: {dateTime: 'dd/mm/yyyy'}},
          entityName: {selectFrom: EntityDefinitions.getEntityDefinitionsAsSelectFromOptions}
        }
      }
    };
    def.fields = [
      {name: 'id', label: '#', width: 1},
      {name: 'when', formula: function(rev) { return dateFilter(rev.when, 'dd/MM/yyyy HH:mm:ss'); }, label: 'Πότε', width: 2},
      {name: 'who', formula: function(rev) { return rev.who.fullName; }, label: 'Ποιός', width: 2},
      {name: 'modifiedEntityNames', formula: function(rev) { return entityChangesSummary(rev.modifiedEntityNames);}, label: 'Οντότητες', width: 5}
    ];
    return def;
  }]);


  /* replace the following with this
   module.factory('RevisionAPI',['$resource', 'apiService',function($resource, apiService) {
   return $resource(apiService.apiPathname + 'entityRevisions/query/:revisionId',{page:0,size:5},
   {},
   {
   getSummary: {
   method: 'GET',
   url: apiService.apiPathname + 'entityRevisions/query',
   timeout: 5000
   },
   getDetail: {
   method: 'GET',
   url: apiService.apiPathname + 'entityRevisions/query/:revisionId',
   timeout: 5000
   }
   }
   );
   }]);
   */
  module.factory('revisionDetailService',['$resource', 'apiService',function($resource, apiService) {
    return $resource(apiService.apiPathname + 'entityRevisions/query/:revisionId',{page:0,size:5});
  }]);

  module.factory('revisionSummaryService',['$resource', 'apiService',function($resource, apiService) {
    return $resource(apiService.apiPathname + 'entityRevisions/query',{page:0,size:5});
  }]);

  module.controller('RevisionFormController', ['$scope', '$location', '$window', 'revisionSummaryService', 'revisionDetailService','EntityDefinitions', 'apiService', function ($scope, $location, $window, revisionSummaryService, revisionDetailService, EntityDefinitions, apiService) {
      var page = 0;
      var revisionId = $location.path().split('/')[2];

      $scope.close = function() {
        $window.history.back();
      };

      $scope.entityChanges = [];
      $scope.loading = true;
      $scope.getNextEntityChange = function() {
          $scope.loading = true;
          revisionDetailService.get({revisionId:revisionId,page:page,size:3}).$promise.then(apiService.retrocycle).then(function(revision) {
            if (revision.content.length > 0) {
              revision.content.forEach(function(content){
                $scope.entityDef = EntityDefinitions.getEntityDefinitionForDomainClass(content.entityName);
                $scope.hasAddedOrRemoved = false;
                $scope.hasNewOrOldValue = false;
                content.propertyChanges.forEach(function(field){
                  var fieldDef = EntityDefinitions.findFieldDefinitionFor($scope.entityDef, field.path);
                  if(fieldDef!== undefined) {
                    field.path = fieldDef.label;
                  } else {
                    console.warn('Missing label for field:'+field.path);
                  }
                  if (field.added) {
                    $scope.hasAddedOrRemoved = true;
                    field.added = EntityDefinitions.formatFieldValue(field.added,fieldDef);
                  }
                  if (field.removed) {
                    $scope.hasAddedOrRemoved = true;
                    field.removed = EntityDefinitions.formatFieldValue(field.removed,fieldDef);
                  } else  {
                    $scope.hasNewOrOldValue = true;
                    if( field.newValue !== undefined && field.newValue !== null) {
                      field.newValue = EntityDefinitions.formatFieldValue(field.newValue, fieldDef);
                    }
                    if (field.oldValue !== undefined && field.oldValue !== null) {
                      field.oldValue = EntityDefinitions.formatFieldValue(field.oldValue, fieldDef);
                    }
                  }
                });
              });
            }
            $scope.entityChanges = $scope.entityChanges.concat(revision.content);
            $scope.loading=false;
          });
          page = page+1;
        };
      revisionSummaryService.get({q:revisionId}).$promise.then(function(revision) {
        $scope.summary = revision.content[0];
        $window.document.title = $scope.entityDefinition.name.singular +' - ' + $scope.summary.id + ' - ' + appConstants.appName;
      });

      $scope.loadMore = function() {
        if(page <= $scope.entityChanges.length){
          $scope.getNextEntityChange();
        }
      };
      $scope.getNextEntityChange();
    }]);

  ['PasswordPolicy', {entityName: 'UserProfile', options: userProfileOptions}, {entityName: 'User', options: userOptions}, 'Authority', 'RoleGroup', {entityName: 'EntityRevision', options: {formTemplate: entityRevisionFormTemplate}}].forEach(function(entity) {
    entityCrud.declareAngularStructuresForEntity(module, entity.entityName ? entity.entityName : entity, entity.options);
  });
  return module;
});