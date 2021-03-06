'use strict';

var proxySnippet = require('grunt-connect-proxy/lib/utils').proxyRequest;
var modRewrite = require('connect-modrewrite');

module.exports = function (grunt) {
  require('load-grunt-tasks')(grunt);
  require('time-grunt')(grunt);

  grunt.initConfig({
    yeoman: {
      // configurable paths
      app: require('./bower.json').appPath || 'SECP-service/src/main/resources/assets',
      dist: 'SECP-service/target/classes/assets'
    },
    sync: {
      dist: {
        files: [{
          cwd: '<%= yeoman.app %>',
          dest: '<%= yeoman.dist %>',
          src: '**'
        }]
      }
    },
    watch: {
      options: {
        livereload: 35729
      },
      src: {
        files: [
          '<%= yeoman.app %>/app/*.html',
          '<%= yeoman.app %>/app/css/**/*',
          '<%= yeoman.app %>/app/js/**/*',
          '<%= yeoman.app %>/app/views/**/*'
        ],
        //tasks: ['sync:dist']
      }
    },
    connect: {
      proxies: [
        {
          context: '/SECP',
          host: 'localhost',
          port: 8080,
          https: false,
          changeOrigin: false
        },
        {
          context: '/metrics',
          host: 'localhost',
          port: 8081,
          https: false,
          changeOrigin: false
        },
        {
          ws: true,
          context: '/cometd',
          host: 'localhost',
          port: 8080,
          https: false,
          changeOrigin: true
        }
      ],
      options: {
        port: 9000,
        // Change this to '0.0.0.0' to access the server from outside.
        hostname: 'localhost',
        livereload: 35729
      },
      livereload: {
        options: {
          open: true,
          base: [
            '<%= yeoman.app %>/app'
          ],
          middleware: function (connect, options) {
            var middlewares = [];
                    middlewares.push(modRewrite([
                      '!/SECP|/assets|\\.html|\\.js|\\.svg|\\.css|\\.png|\\woff|\\ttf|\\swf|\\.jpg$ /index.html'
                    ]));
                    options.base.forEach(function (base) {
                      // Serve static files.
                      middlewares.push(connect.static(base));
                    });
                    middlewares.push(proxySnippet);
                    return middlewares;
          }
        }
      },
      /*
      dist: {
        options: {
          base: '<%= yeoman.dist %>'
        }
      }
      */
    },
    // Put files not handled in other tasks here
    copy: {
      dist: {
        files: [{
          expand: true,
          dot: true,
          cwd: '<%= yeoman.app %>',
          dest: '<%= yeoman.dist %>',
          src: '**'
        }]
      },
    },
    // Test settings
    karma: {
      unit: {
        configFile: '<%= yeoman.app %>/test/config/karma.conf.js',
        singleRun: true
      }
    },
    bowercopy: {
      options: {
        destPrefix: '<%= yeoman.app %>'
      },
      test: {
        files: {
          'test/lib/angular-mocks': 'angular-mocks',
          'test/lib/angular-scenario': 'angular-scenario'
        }
      }
    }
  });

  grunt.registerTask('server', function (target) {
    grunt.task.run([
      //'copy:dist',
      'configureProxies',
      'connect:livereload',
      'watch'
    ]);
  });
};
