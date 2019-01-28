<?php  ?>
<!DOCTYPE html>
<html>
<head>
	<title>Live Tracking</title>

	<script src="https://www.gstatic.com/firebasejs/5.8.1/firebase.js"></script>
	<script src="https://www.gstatic.com/firebasejs/5.8.1/firebase-app.js"></script>
	<script src="https://www.gstatic.com/firebasejs/5.8.1/firebase-database.js"></script>
	<script src="http://maps.google.com/maps/api/js?sensor=false" type="text/javascript"></script>
    <script src="https://code.jquery.com/jquery-3.3.1.min.js" integrity="sha256-FgpCb/KJQlLNfOu91ta32o/NMZxltwRo8QtmkMRdAu8=" crossorigin="anonymous"></script>

    <script type="text/javascript">
        //http://localhost/livetracking/?date=2019-01-27&id=9c3b3d742f207073&filter=true
    	var getUrlParameter = function getUrlParameter(sParam) {
    	    var sPageURL = window.location.search.substring(1),
    	        sURLVariables = sPageURL.split('&'),
    	        sParameterName,
    	        i;

    	    for (i = 0; i < sURLVariables.length; i++) {
    	        sParameterName = sURLVariables[i].split('=');

    	        if (sParameterName[0] === sParam) {
    	            return sParameterName[1] === undefined ? true : decodeURIComponent(sParameterName[1]);
    	        }
    	    }
    	};

    	function setCookie(cname, cvalue, exdays) {
    	  var d = new Date();
    	  d.setTime(d.getTime() + (exdays*24*60*60*1000));
    	  var expires = "expires="+ d.toUTCString();
    	  document.cookie = cname + "=" + cvalue + ";" + expires + ";path=/";
    	} 

    	function getCookie(cname) {
    	  var name = cname + "=";
    	  var decodedCookie = decodeURIComponent(document.cookie);
    	  var ca = decodedCookie.split(';');
    	  for(var i = 0; i <ca.length; i++) {
    	    var c = ca[i];
    	    while (c.charAt(0) == ' ') {
    	      c = c.substring(1);
    	    }
    	    if (c.indexOf(name) == 0) {
    	      return c.substring(name.length, c.length);
    	    }
    	  }
    	  return "";
    	}

    	
    </script>

	<script>
	  // Initialize Firebase
	  var config = {
	    apiKey: "AIzaSyDx474ENgKZMcwr2JbMFxKZsi7IUEWg-Yk",
	    authDomain: "locationtracking-b6067.firebaseapp.com",
	    databaseURL: "https://locationtracking-b6067.firebaseio.com",
	    projectId: "locationtracking-b6067",
	    storageBucket: "locationtracking-b6067.appspot.com",
	    messagingSenderId: "912972956272"
	  };
	  firebase.initializeApp(config);
	</script>
</head>

<body>


	<div id="map" style="width: 1100px; height: 500px;"></div>
    

    <script>

    	var mobileID = getUrlParameter("id");
    	var date = getUrlParameter("date");
    	var filter = getUrlParameter("filter");

		var ref = firebase.database().ref("locations/"+mobileID+"/"+date);
		var mdata = [];
		var finaldata = [];
		var tempArray = [];


		ref.on("value", function(snapshot) {

			snapshot.forEach(function(childSnapshot) {
				var childData = childSnapshot.val();
				var latitude = childData.latitude;
				var longitude = childData.longitude;
				var datetime = childData.created_at;
				var still = childData.still;
				var push = true;


				var tempObj = {still:still,lat:latitude,lng:longitude,dt:datetime};
				finaldata.push(tempObj);




                if(filter == "true"){
                	if(still == true){
					
						tempArray.push(tempObj);
						if(tempArray.length >1){
							push = false;
						}
					} else{

						push = true;
						tempArray = [];
					}
                }


				if(push){
					mdata.push(tempObj);
				}
			});

			console.log("finaldata:"+finaldata.length); 
			console.log("mdata:"+mdata.length); 


			if(mdata.length >0){

				var map = new google.maps.Map(document.getElementById('map'), {
				  zoom: 15,
				  center: new google.maps.LatLng(mdata[0].lat, mdata[0].lng),
				  mapTypeId: google.maps.MapTypeId.ROADMAP
				});
				var infowindow = new google.maps.InfoWindow();
				var marker, i;


				$.each( mdata, function(key,value) {
				  //console.log(value);
				  marker = new google.maps.Marker({
				    position: new google.maps.LatLng(value.lat,value.lng),
				    map: map
				  });

				  google.maps.event.addListener(marker, 'click', (function(marker, i) {
				    return function() {
				     /* var milis = parseInt(value.unix_timestamp);
				      var date = new Date(milis);
				      
				      console.log(date);*/
				      infowindow.setContent(value.dt+","+value.still);
				      infowindow.open(map, marker);
				    }
				  })(marker, i));
				});
			}
		});
    </script>
</body>
</html>