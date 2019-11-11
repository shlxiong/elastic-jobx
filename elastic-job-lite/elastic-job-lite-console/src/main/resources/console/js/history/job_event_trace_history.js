$(function() {
    $("[data-mask]").inputmask();
    $(".toolbar input").bind("keypress", function(event) {
        if("13" == event.keyCode) {
            $("#job-exec-details-table").bootstrapTable("refresh", {silent: true});
        }
    });
    $("#job-exec-details-table").bootstrapTable({
    	url: ctxPath+"/api/tracelog/taskinfo",
    	cache: false,
    }).on("all.bs.table", function() {
        doLocale();
    });
});

function queryParams(params) {
    var sortName = "success" === params.sortName ? "isSuccess" : params.sortName;
    return {
        pageSize: params.pageSize,
        pageNo: params.pageNumber,
        q: params.searchText,
        sort: sortName,
        order: params.sortOrder,
        jobName: $("#job-name").val(),
        startTime: $("#start-time").val(),
        endTime: $("#end-time").val(),
        instanceId: $("#instance-id").val(),
        //isSuccess: $('input[name = "isSuccess"]:checked ').val()
    };
}

/*
function successFormatter(value) {
    switch(value) {
    case true:
        return "<span class='label label-success' data-lang='execute-result-success'></span>";
      case false:
          return "<span class='label label-danger' data-lang='execute-result-failure'></span>";
      default:
        return "<span class='label label-danger' data-lang='execute-result-null'></span>";
    }
}*/
function splitJobState(value){
	return value.substring("TASK_".length, value.length);
}
function splitExecutSource(value){
	if (value == "NORMAL_TRIGGER"){
		value = "NORMAL";   //FAILOVER, MISFIRE
	}
	return value;
}
function viewJobOptions(value){
	var htmlStr = '<a href="javascript:void(0);" onclick="showViewMenu(event)">'+value+'</a>';
	htmlStr += '<ul class="treeview-menu" style="display:none">'
            + '<li><a href="#" class="sub-menu" onclick=""><i class="fa" data-lang="operation-view-status"></i></a></li>'
            + '<li><a href="#" class="sub-menu"><i class="fa" data-lang="operation-view-execution"></i></a></li>'
            + '</ul>';
	return htmlStr;
}
function showViewMenu(){
	var targetObj = $(event.srcElement || event.target);
	targetObj.next().toggle();
	var taskId = targetObj.parents("TR").attr("data-uniqueid");
	var viewLinks = targetObj.next().find(".sub-menu");
	$(viewLinks[0]).unbind().bind("click", function(){
		$("#history-status-modal").modal("show");
		var showUrl = ctxPath+"/api/tracelog/status?taskId="+taskId;
		$("#job-exec-status-table").bootstrapTable('refresh', {url: showUrl});
		targetObj.next().toggle();
	});
	
	$(viewLinks[1]).unbind().bind("click", function(){
		$("#history-execution-modal").modal("show");
		var showUrl = ctxPath+"/api/tracelog/execution?taskId="+taskId;
		$("#job-exec-execution-table").bootstrapTable('refresh', {url: showUrl});
		targetObj.next().toggle();
	});
}

function splitFormatter(value) {
    var maxLength = 50;
    var replacement = "...";
    if(null != value && value.length > maxLength) {
        var vauleDetail = value.substring(0 , maxLength - replacement.length) + replacement;
        value = value.replace(/\r\n/g,"<br/>").replace(/\n/g,"<br/>").replace(/\'/g, "\\'");
        return '<a href="javascript: void(0);" style="color:#FF0000;" onClick="showHistoryMessage(\'' + value + '\')">' + vauleDetail + '</a>';
    }
    return value;
}
