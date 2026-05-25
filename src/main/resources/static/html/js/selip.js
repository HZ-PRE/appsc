window.selip = {
	data: {
		selectedValue: "0",
		selectedInfo: 178
	},
	/* 初始化执行 */
	created() {
	},
	methods: {
		myIpSelect(e){
			let o=e.target.selectedOptions[0];
			app.data.selectedValue=o.value;
			selectedInfo=Number(o.dataset.extraInfo);
			app.data.selectedInfo = (Math.ceil((60 / selectedInfo) * 1000)) + 177;
			apiSum.innerText = `数据来源：免费接口每分钟最多查询 ${Math.floor(60000/app.data.selectedInfo)} 次`;
		},
		copyQueryIPs(){
			const tbody = document.querySelector('#resultTable');
			let text = '';
			// 遍历每一行
			tbody.querySelectorAll('tr').forEach(tr => {
			  const cells = [...tr.querySelectorAll('td')].map(td => td.innerText.trim());
			  text += cells.join('\t') + '\n'; // 用制表符分隔，换行符结尾
			});
			copyText(text.trim());
		},
		//更新插件
		loadIpDb:throttle(() => {
			autolog.warn("ip插件更新正在更新中", 10000);
			getUpdateApp("ip/getIpDb", "ip插件更新", null, "selip-result");
		}, 60000),
		getIpApi(api, ip) {
			let ipApi = `${appscApi}api/ip/lookup/${ip}`;
			switch (api) {
				case '1':
					ipApi = `https://api.ip.sb/geoip/${ip}`;
					break;
				case '2':
					ipApi = `https://qifu-api.baidubce.com/ip/geo/v1/district?ip=${ip}`;
					break;
				case '3':
					ipApi = `http://ip-api.com/json/${ip}?lang=zh-CN`;
					break;
				default:
					// 无匹配时的默认代码
					ipApi = `${appscApi}api/ip/lookup/${ip}`;
			}
			return ipApi;
		},
		async queryIPs() {
			const input = document.getElementById("ipList").value.trim();
			const ips = input.split('\n').map(ip => ip.trim()).filter(ip => ip);
			if (ips.length === 0) {
				alert("请输入至少一个 IP 地址");
				return;
			}

			const table = document.getElementById("resultTable");
			const tbody = table.querySelector("tbody");
			tbody.innerHTML = "";
			table.style.display = "table";
			let isSuccess = false;
			for (const ip of ips) {
				const tr = document.createElement("tr");
				tr.innerHTML = `<td>${ip}</td><td colspan="3">查询中...</td>`;
				tbody.appendChild(tr);
				try {
					const res = await fetch(this.getIpApi(app.data.selectedValue, ip));
					let data = await res.json();
					if ((data['status'] && data['status'] === "success") || data['continent_code']) {
						isSuccess = true;
					} else if (data['code'] && (data['code'] === "Success" || data['code'] === 200)) {
						isSuccess = true;
						data = data.data;
					}
					if (isSuccess) {
						tr.innerHTML =
							`<td>${ip}</td><td>${(data.country ? data.country + "  ":"") + (data.continent ? data.continent:"")}</td><td>${(data.region ? data.region+"  ":"") +(data.city ? data.city:"")}</td><td>${data.isp}</td>`;
					} else {
						tr.innerHTML = `<td>${ip}</td><td colspan="3">查询失败：${data.message || "未知错误"}</td>`;
					}
				} catch (e) {
					tr.innerHTML = `<td>${ip}</td><td colspan="3">请求错误</td>`;
				}

				await new Promise(r => setTimeout(r, app.data.selectedInfo)); // 避免超过免费速率限制
			}
		}
	}
};
