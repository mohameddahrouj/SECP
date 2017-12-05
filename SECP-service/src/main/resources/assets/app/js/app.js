// Declare app level module which depends on filters, and services
angular.module('SECP', ['ngResource', 'ngRoute', 'ui.bootstrap', 'ui.date', 'routeStyles', 'angular-jwt'])
  .config(function ($routeProvider, $locationProvider, jwtOptionsProvider, $httpProvider) {

    //configuring authentication
    jwtOptionsProvider.config({
        unauthenticatedRedirectPath: '/login',
        tokenGetter: ['Auth', function(Auth) {
            if (Auth.isTokenExpired()) {
                return null;
            }

            return localStorage.getItem('token');
        }]
    });
    $httpProvider.interceptors.push('jwtInterceptor');


    $routeProvider
      .when('/', {
        templateUrl: 'views/home/home.html',
        controller: 'HomeController',
        css: 'css/home.css',
      })
      .when('/login', {
        templateUrl: 'views/login/login.html',
        controller: 'LoginController',
        css: 'css/login.css',
      })
      .when('/register', {
        templateUrl: 'views/register/register.html',
        controller: 'RegisterController',
        css: 'css/register.css',
      })
      .when('/chats', {
        templateUrl: 'views/chat/chats.html',
        controller: 'ChatController',
        css: 'css/chat.css',
        requiresLogin: true
      })
      .otherwise({ redirectTo: '/' });

      // use the HTML5 History API
      $locationProvider.html5Mode({
             enabled: true,
             requireBase: false
      });
  })
  .run(function($rootScope,Auth,authManager) {
      $rootScope.logout = function() {
        Auth.logout();
        location.reload();
      }

      authManager.checkAuthOnRefresh();
      authManager.redirectWhenUnauthenticated();
  });

