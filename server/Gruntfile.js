module.exports = function(grunt) {
  grunt.initConfig({
    pkg: grunt.file.readJSON('package.json'),
    express: {
      dev: {
        options: {
          script: 'app.js'
        }
      }
    },
    watch: {
      express: {
        files: [ 'app.js', 'routes/*.js' ],
        tasks: [ 'express:dev' ],
        options: {
          spawn: false
        }
      }
    }
  });

  grunt.loadNpmTasks('grunt-express-server');
  grunt.loadNpmTasks('grunt-contrib-watch');
  grunt.registerTask('default', [ 'express:dev', 'watch' ]);
};
