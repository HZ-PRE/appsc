window.yun = {
	data: {
		yunDirs:[]
	},
	/* 初始化执行 */
	created() {
		let that =this;
		const appTypeConf = document.getElementById("app-type-conf");
		for (var index = 0; index < APPCONFSUM; index++) {
			appTypeConf.innerHTML += `<option value="${index}">配置${index}</option>`;
		
		}
		let yunDirs= localStorage.getItem("yun-dir-list-2026511");
		if(yunDirs){
			let dirs=JSON.parse(yunDirs);
			that.methods.getYunDirList(dirs);
			app.data.yunDirs = dirs;
		}
		const fileInput = document.getElementById("file");
		const fileName = document.getElementById("file-name");
		fileInput.addEventListener("change", function() {
			const button = document.querySelector('[type="submit"]')
			document.getElementById("result").innerText = "";
			button.style.display = "inline-block";
			fileName.textContent = this.files.length > 0 ? this.files[0].name : "未选择文件";
			if (fileName.textContent.includes("（") || fileName.textContent.includes("）")) {
				button.style.display = "none";
				document.getElementById("result").innerText = "❌ 文件名不能含有空格和（）与 ()";
			}
		});
		const submitBut = document.querySelector('[type="submit"]')
		document.getElementById("uploadForm").addEventListener("submit", async function(event) {
			event.preventDefault();
			submitBut.style.display = "none";
			const form = event.target;
			const formData = new FormData(form);
			formData.append("appReleaseType", "0");
			document.getElementById("result").innerText = "✅ 请不要关闭窗口，正在上传中... ";
			that.methods.ossUploadFun(formData);
		});
	},
	methods: {
		getYunDirList(list){
			const yunDirList = document.getElementById("yun-dir-list");
			for (var index = 0; index < list.length; index++) {
				if(!app.data.yunDirs.includes(list[index])){
					app.data.yunDirs.push(list[index]);
					yunDirList.insertAdjacentHTML("beforeend",
						`<option value="${list[index]}"></option>`
					);
				}
			}
			localStorage.setItem("yun-dir-list-2026511",JSON.stringify(app.data.yunDirs.slice(-10)));
		},
		toggleOtherConf() {
		  const conf = document.getElementById('other-conf');
		  conf.classList.toggle('show');
		},
		toggleLogs() {
		  let aa= async function() { 
			  try {
		          const res = await fetch("https://download.bzyvpn.net/exelogs/appsc_link.log");
		          if (!res.ok) {
					  autolog.error("加载失败");
		              return;
		          }
		  
		          const text = await res.text();
				  autolog.confirm("日志",`<pre style="max-height: 400px; overflow: auto; white-space: pre;">${text}</pre>`,null,false,null,{maxWidth:700,isCope:true,copeFun:function (){
						  copyText(text);
					  }});
		      } catch (e) {
				autolog.error("请求异常：" + e);
		      }
			}
			aa()
		},
		ossUploadFun(formData) {
			let that =this;
			let newLocaHash = location.hash.substring(1);
			const submitBut = document.querySelector('[type="submit"]')
			ossUpload(formData, function(e, ret) {
				let innerText = ""
				if (e) {
					that.getYunDirList([formData.get("remoteDir")]);
					if (ret.startsWith("1:")) {
						return autolog.confirm("资源已存在，是否替换", ret, function(r) {
							autolog.warn("正在替换中，请等待...")
							formData.delete("appReleaseType");
							formData.append("appReleaseType", "1");
							safeSetText(newLocaHash,"正在重新上传中...","result");
							r.close();
							that.ossUploadFun(formData);
						}, true, function(r) {
							safeSetText(newLocaHash,"","result");
							if(getPageHash(newLocaHash)) submitBut.style.display = "inline-block";
							r.close();
						});
					} else {
						innerText = "✅ 上传成功:" + ret;
					}
				} else {
					innerText = `❌ 上传失败: ${ret}`;
				}
				safeSetText(newLocaHash,innerText,"result");
				if(getPageHash(newLocaHash)) submitBut.style.display = "inline-block";
			});
		},
		
		setOther() {
			let otherConf = document.getElementById("other-conf");
			otherConf.hidden = !otherConf.hidden
		},
		
		getUpdateAppConfig(t) {
			let data = {
				types: `${t}PrivateKeyPath,${t}BucketName,${t}Endpoint,${t}AccessKeyId,${t}AccessKeySecret,${t}RemoteDir`,
				success: function(e) {
					if (e) {
						for (key in e) {
							let name = lowercaseFirstLetter(key.substring(t.length));
							document.querySelector(`[name=${name}]`).value = e[key];
						}
					};
				}
			}
		
			let types = data.types.split(",");
			for (key of types) {
				let name = lowercaseFirstLetter(key.substring(t.length));
				document.querySelector(`[name=${name}]`).value = "";
			}
			getAppConfig(data);
		},
		onAppType(t, e) {
			this.getUpdateAppConfig(document.querySelector(`[name="appType"]`).value + document.querySelector(`[name="appTypeConf"]`)
				.value);
		}
	}
};
