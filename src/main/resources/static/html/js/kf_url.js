window.kf_url = {
	data: {
		selectedType: 'host_ym.txt',
		selectedVersion: 12,
		passwordRet: '',
		passwordText: '',
		pawjg:'+',
		aesKey:"rwb6c4e7fz$6el%0",
		aesIv:"z1b6c3t4e5f6k7w8"
	},
	/* 初始化执行 */
	created() {
		for (var index = 0; index < APPKFTYPE.length; index++) {
			document.getElementById("app-type-name").insertAdjacentHTML("beforeend",
				`<option value="${index}">${APPKFTYPE[index]["name"]}（${APPKFTYPE[index]["host"]}）</option>`
			);
		}
		this.methods.myPwdChange()
	},
	methods: {
		myPwdChange(e){
			let kf='';
			if(e){
				if(!e.target.value || '' === e.target.value){
					autolog.warn("没有找到对应的文件信息");
					return
				}
				app.data.selectedType = APPKFTYPE[e.target.value]["host"];
				kf=APPKFTYPE[e.target.value]["val"];
			}else {
				app.data.selectedType=APPKFTYPE[0]["host"];
				kf=APPKFTYPE[0]["val"];
			}
			this.setPwdText("");
			let that=this;
			let data ={
				url:`${HOSTOSS[0]}/${app.data.selectedType}?t=${new Date().getTime()}`,
				success: function(el) {
					if (el) {
						el=el.trim();
						app.data.passwordText=el;
						that.setPwdText(el);
						that.loadHostsByAPP(kf,el ? [getDomainFromURL(el)] : []);
					}else{
						autolog.error(el)
					}
				},
				fail: function(el) {
					document.getElementById('password-result').innerHTML = el
					autolog.error(el)
				},
			}
			urlOnLineFile(data);
		},
		copyQueryPwd(){
			copyText(app.data.passwordText.trim());
		},
		myPwdInput(e){
			app.data.passwordText=e.target.value.trim()
		},
		queryPwd() {
			let txt = app.data.passwordText || "";
			txt = txt.replaceAll(" ", "").trim();
		
			if (txt === "") {
				autolog.warn("内容不能为空");
				return;
			}
			try {
				autolog.warn("正在上传中，请等待...",6000);
				document.getElementById('password-result').innerHTML = "正在上传中，请等待...";
				let data ={
					data: {
						url:HOSTOSS.join(','),
						content:txt,
						fileName:app.data.selectedType,
						fileType:'text/plain'
					},
					success: function(e) {
						putServerHostByDomain({
							data: [getDomainFromURL(app.data.passwordText)],
							success(s) {
								autolog.success("数据更新成功");
							},
							fail(error) {
								autolog.error(error);
							}
						});
						document.getElementById('password-result').innerHTML = e;
						autolog.success(`上传成功`)
					},
					fail: function(e) {
						document.getElementById('password-result').innerHTML = e;
						autolog.error(e);
					}
				}
				ossUploadV1(data);
			} catch (e) {
				console.error(e);
				autolog.error("上传失败：" + e.message);
			}
		},
		domainChange(e) {
			const value = e?.target?.value;
			if (!value) return;
			const domain= getDomainFromURL(app.data.passwordText);
			app.data.passwordText = app.data.passwordText.replace(domain,value);
			this.setPwdText(app.data.passwordText);
		},
		setPwdText(text) {
			const el = document.getElementById('pwd-txt');
			if (!el) return;
			el.value = text || "";
			el.dispatchEvent(new Event("input", { bubbles: true }));
		},
		loadHostsByAPP(a, hosts) {
			const useDomain = document.getElementById("use-domain");
			if (!useDomain) return;
			useDomain.innerHTML = "<option value=''>请选择</option>";
			getServersHostByApp({
				app: a,
				success(list) {
					const selectedHosts = new Set(hosts || []);
					const hostsList = Array.isArray(list) ? list : [];
					const fragment = document.createDocumentFragment();

					hostsList.forEach((item) => {
						let domain = item?.domain || "";
						domain = getDomainFromURL(domain);
						if (!domain || selectedHosts.has(domain)) {
							return;
						}
						const option = document.createElement("option");
						option.value = domain;
						option.textContent = domain;
						fragment.appendChild(option);
					});
					useDomain.appendChild(fragment);
				},
				fail(error) {
					autolog.error(error);
				}
			});
		},
	}
};