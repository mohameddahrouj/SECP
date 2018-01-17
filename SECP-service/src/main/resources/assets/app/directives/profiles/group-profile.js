'use strict';

angular.module('SECP')
    .directive('groupProfile', function (Group) {
    return {
        restrict: 'E',
        scope: {
            id: '=',
            modalname: '@'
         },
        templateUrl: 'directives/profiles/group-profile.html',
        link: function ($scope, element, attrs) {
            var modal = document.getElementById("editModal");
            $scope.$watch('id', function(id) {
                  Group.getProfile(id).then(function(group) {
                      if(group) {
                          $scope.group = group;
                      }
                  });
            });

            $("#profileBlock").niceScroll({autohidemode:'leave'});
            $scope.editProfile = function(){
                console.log("we are editting the profile now");

                // CHECK TO SEE IF USER OR GROUP. IF USER, CHECK TO SEE IF THE USER PROFILE BELONGS TO THE PERSON LOOKING AT IT.
                // THEN OPEN EDITABLE. IF GROUP, OPEN EDITABLE.
                // EDITABLE WOULD BE TO EDIT THE IMAGE AND THE NAME
            }
            $scope.closeProfile = function(){
                document.getElementById("chatBlock").setAttribute("class", "col-md-8");
                $("#profileBlock").hide();
            }
        } //DOM manipulation
    };
});