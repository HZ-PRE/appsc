window.host_pwd = {
	data: {
		selectedType: 'host_ym.txt',
		selectedVersion: 12,
		passwordRet: '',
		passwordText: '',
		pawjg: '+',
		aesKey: "rwb6c4e7fz$6el%0",
		aesIv: "z1b6c3t4e5f6k7w8",
		loadToken: 0
	},

	created() {
		this.methods.renderOptions();
		this.methods.myPwdChange();
	},

	methods: {
		renderOptions() {
			const appTypeName = document.getElementById("app-type-name");
			const appTypeFh = document.getElementById("app-type-fh");

			if (appTypeName) {
				appTypeName.innerHTML = "";
				APPTYPENAME.forEach((item, index) => {
					const option = document.createElement("option");
					option.value = String(index);
					option.textContent = `${item.name}，${item.host}，`;
					appTypeName.appendChild(option);
				});
			}

			if (appTypeFh) {
				appTypeFh.innerHTML = "";
				FUHAO.forEach((item) => {
					const option = document.createElement("option");
					option.value = item;
					option.textContent = item;
					appTypeFh.appendChild(option);
				});
			}
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
						const domain = item?.domain || "";
						let domain1 = domain.startsWith("https://") || domain.startsWith("http://")? domain: `https://${domain}`;
						if (!domain1 || selectedHosts.has(domain1)) {
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

		myPwdChange(e) {
			const state = window.host_pwd.data;
			const selectedIndex = Number(e?.target?.value || 0);
			const selectedApp = APPTYPENAME[selectedIndex] || APPTYPENAME[0];

			if (!selectedApp?.host) {
				autolog.warn("没有找到对应的文件信息");
				return;
			}

			const token = ++state.loadToken;
			state.selectedType = selectedApp.host;
			state.passwordText = "";
			this.setPwdText("");

			urlOnLineFile({
				url: `${HOSTOSS[0]}/${state.selectedType}?t=${Date.now()}`,
				success: (content) => {
					if (!content) {
						autolog.error(content);
						return;
					}

					(async () => {
						try {
							let decText = await aes128CbcDecryptFromHex(content, state.aesKey, state.aesIv);
							if (token !== state.loadToken || appNewLocaHash !== "host_pwd") {
								return;
							}

							if (decText.includes(",")) {
								decText = decText.replaceAll(",", "\n");
								state.pawjg = ",";
							} else if (decText.includes("+")) {
								decText = decText.replaceAll("+", "\n");
								state.pawjg = "+";
							}

							decText = decText.replaceAll(" ", "").trim();
							state.passwordText = decText;
							this.setSeparator(state.pawjg);
							this.setPwdText(decText);
							this.loadHostsByAPP(selectedApp.val, decText ? decText.split("\n") : []);
						} catch (error) {
							this.setResult(error?.message || error);
							autolog.error(error);
						}
					})();
				},
				fail: (error) => {
					this.setResult(error);
					autolog.error(error);
				}
			});
		},

		domainChange(e) {
			const value = e?.target?.value;
			if (!value) return;

			const state = window.host_pwd.data;
			const list = state.passwordText ? state.passwordText.split("\n") : [];
			const domain = value.startsWith("https://") || value.startsWith("http://")
				? value
				: `https://${value}`;

			list.push(domain);
			state.passwordText = [...new Set(list)].join("\n");
			this.setPwdText(state.passwordText);
		},

		copyQueryPwd() {
			copyText((window.host_pwd.data.passwordText || "").trim());
		},

		myPwdInput(e) {
			window.host_pwd.data.passwordText = e?.target?.value?.trim() || "";
		},

		myPwdFhChange(e) {
			window.host_pwd.data.pawjg = e?.target?.value?.trim() || "+";
		},

		queryPwd() {
			const state = window.host_pwd.data;
			let txt = state.passwordText || "";
			txt = txt.replaceAll(/\r?\n/g, state.pawjg);
			txt = txt.replaceAll(" ", "").trim();

			if (txt === "") {
				autolog.warn("加密内容不能为空");
				return;
			}

			try {
				let uniqueArr = [...new Set(txt.split(state.pawjg).filter(Boolean))];
				txt = shuffleArray(uniqueArr).join(state.pawjg);
				autolog.confirm("请检查", `<span>${this.escapeHtml(txt)}</span>`, (dialog) => {
					dialog.close();
					autolog.warn("正在上传中，请等待...", 6000);
					this.setResult("正在上传中，请等待...");

					(async () => {
						const encHex = await aes128CbcEncryptToHex(txt, state.aesKey, state.aesIv);
						ossUploadV1({
							data: {
								url: HOSTOSS.join(','),
								content: encHex,
								fileName: state.selectedType,
								fileType: 'text/plain'
							},
							success: (message) => {
								let uKeys = [];
								for (const uKey in uniqueArr) {
									uKeys.push(uniqueArr[uKey].replaceAll("http://","").replaceAll("https://","").trim())
								}
								putServerHostByDomain({
									data: uKeys,
									success(s) {
										autolog.success("数据更新成功");
									},
									fail(error) {
										autolog.error(error);
									}
								});
								this.setResult(message);
								autolog.success("上传成功");
							},
							fail: (error) => {
								this.setResult(error);
								autolog.error(error);
							}
						});
					})();
				}, true);
			} catch (error) {
				autolog.error("加密失败：" + error.message);
			}
		},

		setPwdText(text) {
			const el = document.getElementById('pwd-txt');
			if (!el) return;
			el.value = text || "";
			el.dispatchEvent(new Event("input", { bubbles: true }));
		},

		setSeparator(value) {
			const el = document.getElementById('app-type-fh');
			if (el) {
				el.value = value || "+";
			}
		},

		setResult(text) {
			const el = document.getElementById('password-result');
			if (el) {
				el.innerHTML = text ?? "";
			}
		},

		escapeHtml(value) {
			const div = document.createElement("div");
			div.textContent = value ?? "";
			return div.innerHTML;
		}
	}
};
