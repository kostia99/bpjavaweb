$(document).ready(function() {
 
    //Stops the submit request
    $("#requestName").submit(function(e){
           e.preventDefault();
    });
    
    //checks for the button click event
    $("#searchButton").click(function(e){
           
            //get the form data and then serialize that
            dataString = $("#requestName").serialize();
            
            //get the form data using another method 
            if ($("input#inputName").val() === undefined || $("input#inputName").val().length < 3) {
            	$("#lookupResults").html("");
            	$("#lookupResults").append("<br /> Name is either too short or not defined." );
            } else {	
	            var inputName = $("input#inputName").val(); 
	            dataString = "inputName=" + "0 " + inputName;
	            
	            getLookupResults(dataString);
            }
    });
});


$(document).on({
	// hides page;
    ajaxStart: function() { $("body").addClass("loading");    },
     ajaxStop: function() { $("body").removeClass("loading"); }    
}); 

var mapCoordinates = [];

function getLookupResults(dataString) {
	$.ajax({
        type: "GET",
        url: "affiliations",
        data: dataString,
        dataType: "json",
        
        //if received a response from the server
        success: function(data, textStatus, jqXHR) {
            //our country code was correct so we have some information to display
             if(data.success){
                 $("#lookupResults").html("");
                 
                 $("#lookupResults").append("<p>");
                 $("#lookupResults").append(data.results.starttext + " <br />");
                 
                 $.each(data.results.authors, function(){
                	 $("#lookupResults").append("<input id=\"insteadButton\" type=\"button\" value=\"Show\" onclick=\"getLookupResults('" + "inputName=" + this + "')\" />");
                	 $("#lookupResults").append(this + " <br />");
                 });
                 
                 $("#lookupResults").append("</p>");
                 
                 $("#lookupResults").append("<p>");
                 $("#lookupResults").append(data.results.keywords + " <br />");
                 $("#lookupResults").append(data.results.datatext + " <br />");
                 $("#lookupResults").append("</p>");
                 
                 $("#lookupResults").append("<p>");
                 $("#lookupResults").append("Keywords changes overtime: <br />");
                 $.each(data.results.keywordsChanges, function() {
                	 $("#lookupResults").append(this + " <br />");
                 });
                 $("#lookupResults").append("</p>");
                 
                 $("#lookupResults").append("<h3>Movements:</h3>");
                 $("#lookupResults").append("<table style=\"width:500px\">");
                 $("#lookupResults").append("<thead>");
                 $("#lookupResults").append("<tr>");
                 $("#lookupResults").append("<th>Workplace</th>");
                 $("#lookupResults").append("<th>Period</th>");
                 $("#lookupResults").append("<th>Work connections</th>");
                 $("#lookupResults").append("</tr>");
                 $("#lookupResults").append("</thead>");
                 
                 $("#lookupResults").append("<tbody>");
                 $.each(data.results.data, function() {
                	 $("#lookupResults").append("<tr>");
                	 $("#lookupResults").append("<td>" + this.workplace.name + "</td>");
                	 $("#lookupResults").append("<td>" + this.period + "</td>");
                	 
                	 var connections = [];
                	 var fromLatitude = this.workplace.latitude;
                	 var fromLongitude = this.workplace.longitude;
                	 var fromname = this.workplace.name;
                	 
                	 var workedwithTable = "";
                	 $.each(this.workedWith, function() {	
                		 workedwithTable = workedwithTable + this.name + " : " + this.connections + " publications <br />";
                		 
                		 connections.push({toname: this.name,
			 				  			   tolatitude: this.latitude,
			 				  			   tolongitude: this.longitude
                		 				 });
                	 });
                	 
                	 mapCoordinates.push({fromname: fromname,
		 				  				  fromlatitude: fromLatitude,
		 				  				  fromlongitude: fromLongitude,
		 				  				  connections: connections
	 									});
                	 
                	 $("#lookupResults").append("<td>" + workedwithTable + "</td>");
                	 $("#lookupResults").append("</tr>");
                 });
                 $("#lookupResults").append("</tbody>");
                 $("#lookupResults").append("</table>");
                 $("#lookupResults").append("<br />");
                 
                 $("#lookupResults").append("<input type=\"button\" onclick=\"loadAPI();\" id=\"loadButton\" value=\"Show on map\" />");
                 $("#lookupResults").append("<div id=\"map_canvas\" style=\"width: 100%; height: 100%; border:1px solid black;\"></div>");
                 
             } 
             //display error message
             else {
                 $("#lookupResults").html("<div><b>invalid name</b></div>");
             }
        },
        
        //If there was no resonse from the server
        error: function(jqXHR, textStatus, errorThrown){
             console.log("Something really bad happened " + textStatus);
              $("#lookupResults").html(jqXHR.responseText);
        },
        
        //capture the request before it was sent to server
        beforeSend: function(jqXHR, settings){
            //adding some Dummy data to the request
            settings.data += "&dummyData=whatever";
            //disable the button until we get the response
            $('#searchButton').attr("disabled", true);
        },
        
        //this is called after the response or error functions are finsihed
        //so that we can take some action
        complete: function(jqXHR, textStatus){
            //enable the button 
            $('#searchButton').attr("disabled", false);
        }
	});
}

function loadAPI() {
	document.getElementById("loadButton").value = "loading...";
	document.getElementById("loadButton").disabled = true;
	
	var script = document.createElement("script");
	script.src = "http://www.google.com/jsapi?key=AIzaSyBUgdpqyRRUCiAhejFo2yOdrLb7-7xyi7o&callback=loadMaps";
	script.type = "text/javascript";
	document.getElementsByTagName("head")[0].appendChild(script);
}

function loadMaps() {
	google.load("maps", "2", {"callback" : loaded});
}

function loaded() {
	document.getElementById("loadButton").disabled = false;
	showMap(mapCoordinates);
}

function showMap(mapCoordinates) {
	if (GBrowserIsCompatible()) {
		var map = new GMap2(document.getElementById("map_canvas"));
		
		var myLatlng = new GLatLng(52.334171295166016, 4.866814136505127);
		map.setMapType(G_HYBRID_MAP);
		map.setCenter(myLatlng, 2);
		
		$.each(mapCoordinates, function() {
			var pointfrom = new GLatLng(this.fromlatitude, this.fromlongitude);
			map.addOverlay(new GMarker(pointfrom,  { title: this.fromname }));
			
			$.each(this.connections, function() {
				var pointto = new GLatLng(this.tolatitude, this.tolongitude);
				map.addOverlay(new GMarker(pointto, { title: this.toname }));
				map.addOverlay(new GPolyline([pointfrom, pointto]));
			});
			
		});
		
		document.getElementById("loadButton").value = "Done";
	}
}
  



