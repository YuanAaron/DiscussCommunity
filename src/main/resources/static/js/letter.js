$(function(){
	$("#sendBtn").click(send_letter);
	$(".close").click(delete_msg);
});

function send_letter() {
	$("#sendModal").modal("hide");

    //获取接收人、内容
    var toName=$("#recipient-name").val();
    var content=$("#message-text").val();
    //发送异步请求（POST）
    $.post(
        CONTEXT_PATH+"/letter/send",
        {"toName":toName,"content":content},
        function(data) {
            data=$.parseJSON(data);
            //在提示框中显示返回的消息
			if (data.code==0) {
                $("#hintBody").text("发送成功");
			}else {
                $("#hintBody").text(data.msg);
			}
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

function delete_msg() {
	// TODO 删除数据
    var btn = this;
    var id = $(btn).prev().val(); //这里我还不是很懂
    $.post(
        CONTEXT_PATH + "/letter/delete",
        {"id":id},
        function(data) {
            data = $.parseJSON(data);
            if(data.code == 0) {
                $(btn).parents(".media").remove();
            } else {
                alert(data.msg);
            }
        }
    );
}