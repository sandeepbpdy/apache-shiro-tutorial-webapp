<%--
  ~ Copyright (c) 2013 Les Hazlewood and contributors
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~     http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  --%>
<jsp:include page="include.jsp"/>
<!DOCTYPE html>
<html>
<head>
    <title>Apache Shiro Tutorial Webapp</title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <!-- Add some nice styling and functionality.  We'll just use Twitter Bootstrap -->
    <link rel="stylesheet" href="//netdna.bootstrapcdn.com/bootstrap/3.0.2/css/bootstrap.min.css">
    <link rel="stylesheet" href="//netdna.bootstrapcdn.com/bootstrap/3.0.2/css/bootstrap-theme.min.css">
</head>
<body>
    <div class="container">
        <div class="jumbotron">
            <h1>Apache Shiro Tutorial Webapp</h1>
            <p class="lead">Enhanced with DICOM Image Reading Capabilities</p>
            <hr>
            <p>This application demonstrates Apache Shiro security features and includes DICOM server connectivity for medical image processing.</p>
        </div>
        
        <div class="row">
            <div class="col-md-6">
                <div class="panel panel-primary">
                    <div class="panel-heading">
                        <h3 class="panel-title">DICOM Image Viewer</h3>
                    </div>
                    <div class="panel-body">
                        <p>Connect to DICOM servers and view medical images.</p>
                        <ul>
                            <li>Test DICOM server connectivity</li>
                            <li>Search for patient studies</li>
                            <li>Retrieve and display DICOM images</li>
                            <li>View DICOM metadata</li>
                        </ul>
                        <a href="<%= request.getContextPath() %>/dicom/viewer" class="btn btn-primary btn-lg">
                            Open DICOM Viewer
                        </a>
                    </div>
                </div>
            </div>
            
            <div class="col-md-6">
                <div class="panel panel-info">
                    <div class="panel-heading">
                        <h3 class="panel-title">Configuration</h3>
                    </div>
                    <div class="panel-body">
                        <p>Configure DICOM server connection using system properties:</p>
                        <ul>
                            <li><code>-Ddicom.server.hostname=your-server</code></li>
                            <li><code>-Ddicom.server.port=11112</code></li>
                            <li><code>-Ddicom.calling.aet=WEBAPP</code></li>
                            <li><code>-Ddicom.called.aet=DCMSERVER</code></li>
                        </ul>
                        <p><small>Default configuration connects to localhost:11112</small></p>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <!-- jQuery (necessary for Bootstrap's JavaScript plugins) -->
    <script src="https://code.jquery.com/jquery.js"></script>
    <script src="//netdna.bootstrapcdn.com/bootstrap/3.0.2/js/bootstrap.min.js"></script>
    <!-- HTML5 Shim and Respond.js IE8 support of HTML5 elements and media queries -->
    <!-- WARNING: Respond.js doesn't work if you view the page via file:// -->
    <!--[if lt IE 9]>
    <script src="https://oss.maxcdn.com/libs/html5shiv/3.7.0/html5shiv.js"></script>
    <script src="https://oss.maxcdn.com/libs/respond.js/1.3.0/respond.min.js"></script>
    <![endif]-->
</body>
</html>
