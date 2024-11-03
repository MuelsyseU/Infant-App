
var recorderId, recorderTime, recorder;

function showTips(html, callback) {
    $("#myModal .modal-body").html(html)
    $("#myModal .modal-cancel").off("click").one("click", () => {
        $('#myModal').modal('hide')
        $('#myModal').off("hidden.bs.modal").one('hidden.bs.modal', (e) =>
            $("#myModal .modal-body").empty());
    })
    $("#myModal .modal-confirm").off("click").one("click", () => {
        $('#myModal').modal('hide')
        $('#myModal').off("hidden.bs.modal").one('hidden.bs.modal', (e) => {
            $("#myModal .modal-body").empty()
            callback()
        });
    })
    $('#myModal').modal('show')
}

// 对于弹出文字的处理
function popupResult(result, size, padding, margin) {
    $(".result").css("font-size", '0')
        .css("padding", '0')
        .css("margin", '4vmin')
    $(".result").text(result).animate({
        "font-size": size,
        "padding": padding,
        "margin": margin
    });
}

var delayPopup

function showResult(result) {
    output = "音频异常，请重试"
    if (result == -1)     output = "非婴儿声线，请重试"
    else if (result == 0) output = "我受惊了"
    else if (result == 1) output = "请换尿布"
    else if (result == 2) output = "我要抱抱"
    else if (result == 3) output = "我饿了"
    else if (result == 4) output = "我困了"
    else if (result == 5) output = "我不舒服"

    clearTimeout(delayPopup)
    if (result >= 0) // Success
        popupResult(output, '12vmin', '4vmin 6vmin', '0');
    else // Exception
        popupResult(output, '5vmin', '0 4vmin', '4vmin');
}

function fileUpload(file) {
    var reader = new FileReader();
    reader.readAsDataURL(file);
    reader.onload = (e) => Android.calcBase64(e.target.result,
        "showResult")
    delayPopup = setTimeout(() => popupResult("识别中，请稍候", '5vmin', '0 4vmin', '4vmin'), 500)
}

var objectURL = ""

function recordStop() {
    clearTimeout(recorderId);
    recorder.stop()
    $("#recordStart").show();
    $("#recordStop").hide();
    $("#fileSelect").removeAttr("disabled");
    $(".tip").animate({
        "font-size":'0'
    });
    objectURL = URL.createObjectURL(recorder.getWAVBlob());
    showTips(`<p>录音结束。是否确认提交？</p><audio src='${objectURL}' controls type='audio/wav' class="d-block mx-auto"/>`, () => {
        URL.revokeObjectURL(objectURL);
        fileUpload(recorder.getWAVBlob());
    });
}

function recordTest() {
//    $(".text").text("CLicked!");
    // import Recorder from '../js-audio-recorder';
    Recorder.getPermission().then(() => {
        console.log('Access granted.');
        recorder = new Recorder();
        recorder.start();
        console.log(recorder);
        $("#recordStart").hide();
        $("#recordStop").show();
        $("#fileSelect").attr("disabled");
        // Animations
        recorderTime = 10
        $(".result").animate({
            "font-size":'0',
            "padding":'0',
            "margin":'4vmin'
        });
        $(".tip").html("音频录制中<br/>10").animate({
            "font-size":'5vmin'
        });
        // Work
        recorderId = setInterval(() => {
//            var notification = new Notification('录音结束', { body: '\n录音时间最长为 10s。点击以查看' });
//            notification.onclick = (event) => { window.electron.ipcRenderer.send('focus') }
//            console.log(notification)
            --recorderTime;
            $(".tip").html(`音频录制中<br/>${recorderTime}`)
            if (recorderTime <= 0) {
                recordStop()
                Android.showAlert('录音结束。录音时长最长不超过 10s。')
            }
        }, 1000)
    }, (error) => {
        console.log(`${error.name} : ${error.message}`);
        showTips("未检测到麦克风权限。是否提供麦克风权限以继续录音？", recordTest);
    });
}

$(document).ready((e) => {
    $("#recordStart").on("click", recordTest);
    $("#recordStop").on("click", recordStop);
    $("#fileSelect").on("click", (e) => $("#fileInput").click())
    $("#fileInput").on("change", (e) => fileUpload(e.target.files[0]))
})