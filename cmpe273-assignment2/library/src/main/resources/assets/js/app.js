$(":button").click(function() {
	var isbn = this.id;
	alert('About to report lost on ISBN ' + isbn);

	$.ajax({
	
	type: 'PUT',
    url: 'http://localhost:8001/library/v1/books/' + isbn + '?status=lost',
    contentType: "application/json",
    success:function(data){
        alert("Success"); 
    	$("#status").text("lost");	
    	$('#'+isbn).attr('disabled','disabled');
       }

	});

});
