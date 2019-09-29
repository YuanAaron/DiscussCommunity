$(function(){
	$("#publishBtn").click(publish);
});

function publish() {
	$("#publishModal").modal("hide");

	//在发送AJAX请求之前，将CSRF令牌设置到请求的消息头中
	var token=$("meta[name='_csrf']").attr("content");
	var name=$("meta[name='_csrf_name']").attr("content");
	$(document).ajaxSend(function(e,xhr,options) {
		xhr.setRequestHeader(name,token);
	});

	//获取标题、内容
	var title=$("#recipient-name").val();
	var content=$("#message-text").val();
	//发送异步请求（POST）
	$.post(
		CONTEXT_PATH+"/discuss/add",
		{"title":title,"content":content},
		function(data) {
			data=$.parseJSON(data);
			//在提示框中显示返回的消息
			$("#hintBody").text(data.msg);
			//显示提示框
            $("#hintModal").modal("show");
            //2s后自动隐藏提示框
            setTimeout(function(){
                $("#hintModal").modal("hide");
                if (data.code==0) {
                    //这句话是刷新了整个页面的，其实更好的做法是前端进行增量更新。
					//因为我们的重点不在前端，所以这里就简化处理了。
                	window.location.reload();
				}
            }, 2000);
		}
	);
}