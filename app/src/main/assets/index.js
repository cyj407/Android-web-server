var ws = null;
var downloadFile = "";
var serverClosed = false;
var fileStart = false;
var fileEnd = true;
var MyBlobBuilder = function() {
    this.parts = [];
};

MyBlobBuilder.prototype.append = function(part) {
    this.parts.push(part);
    this.blob = undefined;
};

MyBlobBuilder.prototype.getBlob = function() {
    if (!this.blob) {
        this.blob = new Blob(this.parts, {
            type: "text/plain"
        });
    }
    return this.blob;
};
var myBlobBuilder = undefined;

function pauseStreaming() {
    ws.send("Stop streaming");
}

function startStreaming() {
    ws.send("Start streaming");
}

function WebSocketTest() {
    if ("WebSocket" in window) {

        if (ws === null || serverClosed === true) {
            serverClosed = false;

            ws = new WebSocket('ws://192.168.137.216:9000');
            ws.binaryData = "blob";

            ws.onopen = function() {
                ws.send("hi, server");
            };

            ws.onmessage = function(event) {
                let rec = event.data;

                if (rec instanceof Blob) {
                    if (fileStart) {
                        myBlobBuilder.append(rec);
                    }
                } else if (typeof rec === "string") {
                    if (rec === "File starts.") {
                        fileStart = true;
                        fileEnd = false;
                        myBlobBuilder = new MyBlobBuilder();
                    } else if (rec === "File ends.") {
                        fileStart = false;
                        fileEnd = true;

                        let bb = myBlobBuilder.getBlob();
                        downloadAndShow(bb);
                        myBlobBuilder = undefined;
                    } else if (rec.substr(0, 7) === "option:") {

                        /* trim the string */
                        rec = rec.replace("option:[", "");
                        rec = rec.replace("]", "");

                        let list_DOM = document.getElementById('optionList');

                        /* remove original datalist */
                        let optNum = list_DOM.options.length;
                        for (let i = 0; i < optNum; ++i) {
                            let opt = document.getElementById('fileOption');
                            if (opt) {
                                opt.parentNode.removeChild(opt);
                            }
                        }

                        /* append new datalist */
                        let optionArray = rec.split(", ");
                        let initOptNum = list_DOM.options.length;
                        for (let i = 0; i < optionArray.length; ++i) {
                            let opt_DOM = document.createElement('option');
                            opt_DOM.value = optionArray[i];
                            opt_DOM.id = "fileOption";
                            list_DOM.appendChild(opt_DOM);
                        }
                    } else {
                        alert(rec);
                    }
                }
            };

            ws.onclose = function() {
                ws.send("exit");
                alert("connection closed.");
                serverClosed = true;
            };
        } else {
            alert("connection exists.");
        }
    } else {
        alert("Your browser is not supported.");
    }
}

function downloadAndShow(rec) {

    /* handle the received file */
    if (rec instanceof Blob) {
        let _span = document.getElementById('rec_container');
        _span.innerHTML = "Receive file: " + downloadFile + "<br>";

        /* create download url of the file */
        let rec_url = window.URL.createObjectURL(rec);

        /* display image */
        if (downloadFile.indexOf('.jpg') !== -1 ||
            downloadFile.indexOf('.png') !== -1) {

            let img_DOM = document.getElementById("rec_img");
            if (img_DOM === null) {
                img_DOM = document.createElement("img");
                img_DOM.id = "rec_img";
                /*_span.innerHTML = "Receive image: ";*/
                _span.appendChild(img_DOM);
            }
            img_DOM.style.cssText = '';
            img_DOM.href = rec_url;
            img_DOM.src = rec_url;
        } else if (downloadFile.indexOf('.mp4') !== -1 ||
            downloadFile.indexOf('.webm') !== -1 ||
            downloadFile.indexOf('.ogv') !== -1) {

            /* hide the previous picture */
            let img_DOM = document.getElementById("rec_img");
            if (img_DOM !== null) {
                img_DOM.style.cssText = 'display: none;';
            }

            let player = document.getElementById("my-player");
            let video = document.getElementById("rec_video");
            if (video === null) {
                video = document.createElement("source");
                video.id = "rec_video";
                player.appendChild(video);
            }
            video.src = rec_url;
            video.type = "video/mp4";
            player.load();
            player.play();
        } else {
            let img_DOM = document.getElementById("rec_img");
            if (img_DOM !== null) {
                img_DOM.style.cssText = 'display: none;';
            }
        }

        /* auto download */
        let download_DOM = document.getElementById('download');
        download_DOM.href = rec_url;
        download_DOM.download = downloadFile;
        download_DOM.style.cssText = '';
        download_DOM.click();
        download_DOM.style.cssText = 'display: none;';

    }

}

function checkWebSocket() {
    if (ws === null || serverClosed === true) {
        alert("Execute WebSocket first.");
        return false;
    }
    return true;
}

function sendFile() {
    if (!checkWebSocket()) {
        return;
    }
    if (ws.readyState === WebSocket.OPEN) {

        if (document.getElementById('uploadFile').files.length === 0) {
            alert("Select a file to send.");
            return;
        }

        /* send filename first */
        let path = document.getElementById('uploadFile').value;
        let startIndex = (path.indexOf('\\') >= 0 ? path.lastIndexOf('\\') : path.lastIndexOf('/'));
        let filename = path.substring(startIndex);
        if (filename.indexOf('\\') === 0 || filename.indexOf('/') === 0) {
            filename = filename.substring(1);
        }
        ws.send("file: " + filename);

        console.log(filename);

        /* send file */
        let file = document.getElementById('uploadFile').files[0];
        let reader = new FileReader();
        reader.onload = function(event) {
            rawData = event.target.result;
        };
        reader.readAsArrayBuffer(file);
        ws.send(file);
    } else {
        alert("webSocket is not ready yet.");
    }

}

function requestFile() {
    if (!checkWebSocket()) {
        return;
    }
    let reqPath = document.getElementById('filePath').value;
    downloadFile = reqPath.substring(reqPath.lastIndexOf('/') + 1);
    ws.send("client request: " + reqPath);
}