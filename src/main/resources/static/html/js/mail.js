window.mail = {
	data: {},
	/* 初始化执行 */
	created() {
		const appTypeConf = document.getElementById("app-type-conf");
		for (var index = 0; index < APPCONFSUM; index++) {
			appTypeConf.innerHTML += `<option value="${index}">配置${index}</option>`;
		
		}
		
		const newLocaHash = location.hash.substring(1);
		const appMsgAsyncsName = new Date().getTime();
		document.getElementById("mailForm").addEventListener("submit", async function(event) {
			event.preventDefault();
			const button = document.querySelector('[type="submit"]')
			button.style.display = "none";
			const form = event.target;
			const formData = new FormData(form);
			document.getElementById("mail-result").innerText = "✅ 请不要关闭窗口，正在执行中... ";
			senMails(formData, function(e, ret) {
				if (e) {
					if("1" === formData.get("importType")){
						var progressTime = setInterval(function() {
							let data = {
								types: `senExtMailProgressName`,
								success: function(e1) {
									if(e1["senExtMailProgressName"] && e1["senExtMailProgressName"]==="true"){
										safeSetText(newLocaHash,"✅邮件已全部发送完成","mail-result", true,appMsgAsyncsName);
										if(getPageHash(newLocaHash)) button.style.display = "inline-block";
										clearInterval(progressTime);
									}else if(e1["senExtMailProgressName"] && e1["senExtMailProgressName"].startsWith("ERR:")){
										safeSetText(newLocaHash,`❌ 执行失败: ${e1["senExtMailProgressName"]}`,"mail-result", true,appMsgAsyncsName);
										if(getPageHash(newLocaHash)) button.style.display = "inline-block";
										if(!e1["senExtMailProgressName"].includes("正在尝试再次发送")){
											clearInterval(progressTime);
										}
									}else if(e1["senExtMailProgressName"]){
										safeSetText(newLocaHash,`邮件已发送到:${e1["senExtMailProgressName"]}`,"mail-result", true,appMsgAsyncsName);
										if(getPageHash(newLocaHash)) button.style.display = "inline-block";
									}else{
										clearInterval(progressTime);
									}
								},
								fail:function(e1) {
									safeSetText(newLocaHash,`❌ 执行失败: ${e1}`,"mail-result", true,appMsgAsyncsName);
									if(getPageHash(newLocaHash)) button.style.display = "inline-block";
								}
							}
							getAppConfig(data);
						}, 3000)
					}else{
						safeSetText(newLocaHash,"✅邮件已全部发送完成","mail-result", true,appMsgAsyncsName);
						if(getPageHash(newLocaHash)) button.style.display = "inline-block";
					}
				} else {
					safeSetText(newLocaHash,`❌ 执行失败: ${ret}`,"mail-result");
					if(getPageHash(newLocaHash)) button.style.display = "inline-block";
				}
			});
		});
		this.methods.getUpdateAppConfig();
	},
	methods: {
		toggleOtherConf() {
		  const conf = document.getElementById('other-conf');
		  conf.classList.toggle('show');
		},
		getLastEmail(){
			let data = {
				types: `senExtMailProgressName`,
				success: function(e) {
					if (e && e['senExtMailProgressName'] && e['senExtMailProgressName'].includes("@")) {
						autolog.confirm(
							"最后发送的邮件",
							e['senExtMailProgressName'],
							success = function(e) {
								e.close();
							})
					}else{
						autolog.warn("没查到最后发送的邮件")
					};
				}
			}
			getAppConfig(data);
		},
		getUpdateAppConfig(t = '') {
			let data = {
				types: `${t}mailSmtpHost,${t}mailSmtpPort,${t}mailSmtpUser,${t}mailSmtpPass,${t}mailUseSsl,${t}mailFromUser`,
				success: function(e) {
					if (e) {
						for (key in e) {
							let name = lowercaseFirstLetter(key.substring(`${t}mail`.length));
							document.querySelector(`[name=${name}]`).value = e[key];
						}
					};
				}
			}
			let types = data.types.split(",");
			for (key of types) {
				let name = lowercaseFirstLetter(key.substring(`${t}mail`.length));
				document.querySelector(`[name=${name}]`).value = "";
			}
			getAppConfig(data);
		},
		onImportType(e){
			if('0'===e.target.value){
				document.getElementById("to-text").hidden=false;
				document.getElementById("to-execl").hidden=true;
			}else{
				document.getElementById("to-text").hidden=true;
				document.getElementById("to-execl").hidden=false;
			}
		},
		onAppType(t,e) {
			this.getUpdateAppConfig(e.target.value);
		}
	}
};