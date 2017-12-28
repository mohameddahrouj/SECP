'use strict';

angular.module('SECP')
  .factory('RegisterService', function($http) {

    return {
        register : function(user) {
            return $http.post("/SECP/admin/register", user)
            .then(function(res) {
                   return res;
            }, function(err) {
                return err;
            });
        }
    }
  });
