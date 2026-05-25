window.instartll = {
	data: {
		appName:"8686/ym.apk"
	},
	/* 初始化执行 */
	created() {
		const appTypeConf = document.getElementById("app-type-conf");
		for (var index = 0; index < APPCONFSUM; index++) {
			appTypeConf.innerHTML += `<option value="${index}">配置${index}</option>`;

		}
		for (var index = 0; index < APPTYPENAME.length; index++) {
			document.getElementById("app-type-name").insertAdjacentHTML("beforeend",
				`<option value="${APPTYPENAME[index]["val"]}">${APPTYPENAME[index]["name"]}（${APPTYPENAME[index]["val"]}）</option>`
			);
		}
		const newLocaHash = location.hash.substring(1);
		document.getElementById("uploadForm")?.addEventListener("submit", async function(event) {
			event.preventDefault();
			const form = event.target;
			const formData = new FormData(form);
			let appName=app.data.appName;
			let flag = true;
			if(appName.endsWith("apk") || appName.endsWith("exe")){
				flag = false;
				appName=appName.replaceAll("appName",formData.get("appType"));
				if("text" === formData.get("appReleaseType")){
					appName=`public/text/${appName}`;
				}
				urlCheck(`https://down.bzyvpn.net/${appName}?text=168`,function (f){
					if (f){
						autolog.confirm("🚨警告⚠️","检测到该安装包已经存在，是否继续上传？",function (e){
							toUpdate(formData);
							e.close();
						},true)
					}else {
						toUpdate(formData);
					}
				})
			}
			if(flag){
				toUpdate(formData)
			}
		});
		function toUpdate(formData){
			const appMsgAsyncsName = new Date().getTime();
			const button = document.querySelector('[type="submit"]')
			button.style.display = "none";
			document.getElementById("result").innerText = "✅ 请不要关闭窗口，正在部署中... ";
			upload(formData, function(e, ret) {
				if (e) {
					var progressTime = setInterval(function() {
						updateProgress(function(d, r) {
							if (d) {
								if (r.includes("true")) {
									safeSetText(newLocaHash,"✅ 安装包上传成功","result");
									clearInterval(progressTime);
									if(getPageHash(newLocaHash)) button.style.display = "inline-block";
								} else {
									safeSetText(newLocaHash,`安装包上传进度: ${r}%`,"result",true,appMsgAsyncsName);
								}
							} else {
								safeSetText(newLocaHash,`❌ 安装包上传失败: ${r}`,"result");
								clearInterval(progressTime);
								if(getPageHash(newLocaHash)) button.style.display = "inline-block";
							}
						})
					}, 10);
				} else {
					safeSetText(newLocaHash,`❌ 安装包上传失败: ${ret}`,"result");
					if(getPageHash(newLocaHash)) button.style.display = "inline-block";
				}
			});
		}
		this.methods.getUpdateAppConfig();
	},
	methods: {
		toggleOtherConf() {
			const conf = document.getElementById('other-conf');
			conf.style.display = conf.style.display === 'none' || conf.style.display === '' ? 'block' : 'none';
		},
		copyDownLink(u) {
			let appReleaseType = document.querySelector('[name="appReleaseType"]').value;
			let appType = document.querySelector('[name="appType"]').value;
			let appName=app.data.appName;
			if(appType){
				appName=appName.replaceAll("appName",appType);
			}
			if("text" === appReleaseType){
				appName=`public/text/${appName}`;
			}
			copyText(`${u}/${appName}`);
		},
		showFileName(e){
			let appReleaseType = document.querySelector('[name="appReleaseType"]').value;
			const fileName = document.getElementById("file-name");
			const button = document.querySelector('[type="submit"]')
			document.getElementById("result").innerText = "";
			button.style.display = "inline-block";
			let textContent = e.target.files.length > 0 ? e.target.files[0].name : "未选择文件";
			fileName.textContent = textContent;
			let appName = "";
			let flag=false;
			let qd=checkForLongNumbers(textContent,4);
			if(qd && qd.length>0){
				appName= qd[0];
			}else{
				flag=true;
			}
			if (flag || textContent.includes("（") || textContent.includes("）")) {
				button.style.display = "none";
				document.getElementById("result").innerText = "❌ 安装包和压缩包名不能含有空格和（）与 (),且安装包必须含有渠道号";
			}else if(appName && (textContent.endsWith("apk") || textContent.endsWith("exe"))){
				let ns= textContent.split(".");
				app.data.appName=`${appName}/appName.${ns[ns.length-1]}`;
			}else if("text" === appReleaseType && textContent){
				let ns= textContent.split(".");
				app.data.appName=`${appName}/appName.${ns[ns.length-1]}`;
			}
		},
		getUpdateAppConfig(t = '') {
			let data = {
				types: `${t}updateAppHost,${t}updateAppUser,${t}updateAppPort,${t}updateAppPrivateKeyPath,${t}updateAppRemoteDir,${t}updateAppCommand`,
				success: function(e) {
					if (e) {
						for (key in e) {
							let name = lowercaseFirstLetter(key.substring(`${t}updateApp`.length));
							document.querySelector(`[name=${name}]`).value = e[key];
						}
					};
				}
			}
			let types = data.types.split(",");
			for (key of types) {
				let name = lowercaseFirstLetter(key.substring(`${t}updateApp`.length));
				document.querySelector(`[name=${name}]`).value = "";
			}
			getAppConfig(data);
		},
		onAppType(t,e) {
			this.getUpdateAppConfig(e.target.value);
		},
		instartllLogs() {
			let aa= async function() {
				try {
					const res = await fetch("https://download.bzyvpn.net/exelogs/log.log");
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
	}
};