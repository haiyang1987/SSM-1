/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
(function() {
  "use strict";

    $('#btn-run-cmd').click(function () {

    var url = '/ssm/v1?op=RUNCOMMAND&cmd=' + $('#cmd').val()

    $.ajax({
        type: 'PUT',
        url: url
    }).then(function(data) {
        for (var i=0;i<data.stdout.length;i++) {
            $('.stdout').append(data.stdout[i]+'<br>');
        }

        for (var i=0;i<data.stderr.length;i++) {
            $('.stderr').append(data.stderr[i]+'<br>');
        }
    });

    });
//
//
//    $('#btn-show-cache').click(function () {
//
//    var url = '/ssm/v1?op=SHOWCACHE'
//
//    $.ajax({
//        type: 'GET',
//        url: url
//    }).then(function(data) {
//       $('.cachestatus').text(data.cacheUsedPercentage);
//    });
//
//    });
        $("#btn-show-cache").click(function() {
                            $("#mychart").toggle();
                        });


})();