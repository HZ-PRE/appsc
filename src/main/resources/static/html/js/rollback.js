window.rollback = {
	data: {},
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
		const remoteDirInput = document.getElementById("remoteDir");
		remoteDirInput.addEventListener("input", function() {
			const button = document.querySelector('[type="submit"]')
			document.getElementById("result").innerText = "";
			button.style.display = "inline-block";
			if (!isPureNumber(this.value.replaceAll(",", ""))) {
				button.style.display = "none";
				document.getElementById("result").innerText = "❌ 多渠道请以,分隔";
			} else {}
		});
		var newLocaHash = location.hash.substring(1);
		document.getElementById("rollbackForm").addEventListener("submit", async function(event) {
			event.preventDefault();
			const button = document.querySelector('[type="submit"]')
			button.style.display = "none";
			const form = event.target;
			const formData = new FormData(form);
			document.getElementById("result").innerText = "✅ 请不要关闭窗口，正在执行中... ";
			updateRollback(formData, function(e, ret) {
				if (e) {
					safeSetText(newLocaHash,"✅执行成功","result");
					if(getPageHash(newLocaHash)) button.style.display = "inline-block";
				} else {
					safeSetText(newLocaHash,`❌ 上传失败: ${ret}`,"result");
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
		getUpdateAppConfig(t = '') {
			let data = {
				types: `${t}rollbackAppHost,${t}rollbackAppUser,${t}rollbackAppPort,${t}rollbackAppPrivateKeyPath,${t}rollbackAppCommand`,
				success: function(e) {
					if (e) {
						for (key in e) {
							let name = lowercaseFirstLetter(key.substring(`${t}rollbackApp`.length));
							document.querySelector(`[name=${name}]`).value = e[key];
						}
					};
				}
			}
			let types = data.types.split(",");
			for (key of types) {
				let name = lowercaseFirstLetter(key.substring(`${t}rollbackApp`.length));
				document.querySelector(`[name=${name}]`).value = "";
			}
			getAppConfig(data);
		},
		
		onAppType(t,e) {
			this.getUpdateAppConfig(e.target.value);
		}
	}
};
