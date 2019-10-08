$(function(){
    bsCustomFileInput.init();
});

$(function(){
    $("form").submit(check_data);
    $("input").focus(clear_error);
    $("#uploadForm").submit(upload);
});

function check_data() {
    var pwd1 = $("#new-password").val();
    var pwd2 = $("#confirm-password").val();
    if(pwd1 != pwd2) {
        $("#confirm-password").addClass("is-invalid");
        return false;
    }
    return true;
}

function clear_error() {
    $(this).removeClass("is-invalid");
}

function upload() {
    //$.post()是对该方法的简化
    $.ajax({
        url: "http://upload-z1.qiniup.com",
        method: "post",
        processData: false, //默认情况下，浏览器会把表单内容转换为字符串提交给服务器，这里表示不要转换。
        //不让JQuery去设置上传的类型（html、json等），浏览器会自动进行设置
        //原因：为了确定文件与其他数据的边界，浏览器会给文件加随机的边界字符串，以便拆分文件与其他数据；
        //如果这里指定了contentType,那么JQuery就会自动去设置类型，边界设置补上，导致上传的文件有问题。
        //手动设置边界key不行，它需要浏览器随机生成，所以设置为false
        contentType: false,
        data: new FormData($("#uploadForm")[0]),
        success: function (data) {
            if (data && data.code==0) {
                //更新头像访问路径
                $.post(
                    CONTEXT_PATH+"/user/header/url",
                    {"fileName": $("input[name='key']").val()},
                    function(data) {
                        data=$.parseJSON(data);
                        if (data.code==0) {
                            window.location.reload();
                        } else {
                            alert(data.msg);
                        }
                    }
                );
            } else {
                alert("上传失败!");
            }
        }
    });

    return false; //事件到此为止，不再向下执行默认底层原有的实现
}