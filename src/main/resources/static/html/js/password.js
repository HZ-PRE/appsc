window.password = {
	data: {
		selectedType: 0,
		selectedVersion: 12,
		passwordRet: '',
		passwordText: ''
	},
	/* 初始化执行 */
	created() {
	},
	methods: {
		myPwdChange(e){
			if('2' === e.target.value){
				copyText(e.target.selectedOptions[0].dataset.extraInfo,'复制成功,请前往浏览器打开。');
			}
			app.data.selectedType = e.target.value;
		},
		copyQueryPwd(){
			copyText(app.data.passwordRet.trim());
		},
		myPwdInput(e){
			app.data.passwordText=e.target.value.trim()
		},
		queryPwd() {
			let data = {
				type: app.data.selectedType,
				version: app.data.selectedVersion,
				text: app.data.passwordText,
				success: function(e) {
					if (e) {
						app.data.passwordRet=e;
						document.getElementById('password-result').innerText = e;
					};
				},
				fail: function(e) {
					document.getElementById('password-result').innerText = e
					autolog.error(e)
				},
			}
			getPassword(data)
		}
	}
};
