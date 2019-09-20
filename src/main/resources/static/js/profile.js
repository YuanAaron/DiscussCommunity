$(function(){
	$(".follow-btn").click(follow);
});

function follow() {
	var btn = this;
	if($(btn).hasClass("btn-info")) {
		// 关注TA
		$.post(
			CONTEXT_PATH+"/follow",
			{"entityType":3,"entityId":$(btn).prev().val()},
			function (data) {
				data=$.parseJSON(data);
				if (data.code==0) {
				    //老师这里没有改按钮的样式，而是直接刷新了页面：这样点击关注/取消关注也会同时刷新关注和关注者的数量，而只
                    //改样式还要修改关注和关注者的数量，比较麻烦（但是真正的前端开发者应该不会直接刷新页面，这一点等我学完js再自己修改）
                    //$(btn).text("已关注").removeClass("btn-info").addClass("btn-secondary");
                    window.location.reload();
				} else {
					alert(data.msg);
				}
            }
		);
	} else {
		// 取消关注
        $.post(
            CONTEXT_PATH+"/unfollow",
            {"entityType":3,"entityId":$(btn).prev().val()},
            function (data) {
                data=$.parseJSON(data);
                if (data.code==0) {
                    //$(btn).text("关注TA").removeClass("btn-secondary").addClass("btn-info");
                    window.location.reload();
                } else {
                    alert(data.msg);
                }
            }
        );
	}
}