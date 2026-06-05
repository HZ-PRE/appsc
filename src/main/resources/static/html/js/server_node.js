window.server_node = {
	data: {
		nodeList:[],
		nodeComt:{
			out_ip:'',
			out_ip_port:'',
			out_ip_user:'',
			out_ip_pwd:'',
			city:'',
			in_ip:'',
			in_ip_port:'',
			in_ip_user:'',
			in_ip_pwd:'',
			dw:0,
			note:'',
		},
		newNode:{},
		trafficMonitorNode: null,
		trafficMonitorNodeT:0,
		searchFilter:{
			search:'',
			isUse:''
		}
	},
	/* 初始化执行 */
	created() {
		for (var index = 0; index < COUNTRIESCN.length; index++) {
			document.getElementById("city-list").insertAdjacentHTML("beforeend",
				`<option value="${COUNTRIESCN[index]}"></option>`
			);
		}
		document.addEventListener('click', function(e) {
			if (!e.target.closest('.action-dropdown')) {
				document.querySelectorAll('.action-dropdown.open').forEach(function(el) {
					el.classList.remove('open');
				});
			}
		});
		this.methods.loadNodes();
	},
	methods: {
		renderList(nodes) {
			let that = this;
			var ul = document.getElementById('nodeList');
			var header = document.querySelector('.list-header');
			ul.innerHTML = '';
			if (!nodes || nodes.length === 0) {
				header.style.display = 'none';
				ul.innerHTML = '<li style="text-align:center;color:var(--text-gray);padding:40px 0;font-size:14px;">暂无节点，请点击右上角添加</li>';
				return;
			}
			header.style.display = 'grid';
			let temps=nodes.slice(0, 30);
			temps.forEach(function(node) {
				var idx = app.data.nodeList ? app.data.nodeList.indexOf(node) : 0;
				var li = document.createElement('li');
				let isUse=node.is_use===0?'(未用)':'(已用)';
				li.className = 'node-item';
				li.innerHTML =
					'<span class="cell-out-ip">' + that.esc(node.out_ip) +isUse+ '</span>' +
					'<span class="cell-in-ip">' + that.esc(node.in_ip) + '</span>' +
					'<span class="cell-country"><span>' + that.esc(node.city) + '</span></span>' +
					'<span class="cell-remark">' + that.esc(node.note) + '</span>' +
					'<span><button class="btn-monitor" data-click="showMonitor(0,' + idx + ')"><svg width="12" height="12" viewBox="0 0 12 12" fill="none"><rect x="1" y="7" width="2" height="4" rx="0.5" fill="currentColor"/><rect x="5" y="4" width="2" height="7" rx="0.5" fill="currentColor"/><rect x="9" y="1" width="2" height="10" rx="0.5" fill="currentColor"/></svg>带宽</button>'+
					'<button class="btn-monitor btn-monitor-traffic" data-click="showMonitor(1,' + idx + ')"><svg width="12" height="12" viewBox="0 0 12 12" fill="none"><path d="M1.5 9.5C3 7.8 4 7 5.2 7c1 0 1.5.5 2.1 1.1.5.5.9.9 1.6.9 1 0 1.8-.9 2.6-2.2" stroke="currentColor" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round"/><path d="M9.8 6.8h1.7v1.7" stroke="currentColor" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round"/></svg>流量</button></span>' +
					'<span class="action-dropdown">' +
						'<button class="dropdown-toggle" onclick="server_node.methods.toggleDropdown(this)">操作 <svg width="12" height="12" viewBox="0 0 12 12"><path d="M3 5l3 3 3-3" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round"/></svg></button>' +
						'<div class="dropdown-menu">' +
  							'<div><button data-click="editNode(' + idx + ')">修改</button><button data-click="showDetail(' + idx + ')">详情</button></div>' +
							'<div><button data-click="subNodeOP(' + idx + ',7)">IP信息</button><button data-click="subNodeOP(' + idx + ',11)">CPU信息</button></div>' +
							'<div><button data-click="subNodeOP(' + idx + ',12)">负载情况</button><button data-click="subNodeOP(' + idx + ',13)">内存情况</button></div>' +
							'<button data-click="subNodeOP(' + idx + ',14)">硬盘情况</button>' +
							'<div><button data-click="subNodeOP(' + idx + ',4)">配置gost</button><button data-click="subNodeOP(' + idx + ',9)">gost情况</button></div>' +
							'<div><button data-click="subNodeOP(' + idx + ',1)">初始化Xrary</button><button data-click="subNodeOP(' + idx + ',5)">初始化Xrary的日志</button></div>' +
							'<div><button data-click="subNodeOP(' + idx + ',3)">配置Xrary</button><button data-click="subNodeOP(' + idx + ',8)">Xrary状态</button></div>' +
							'<div><button data-click="subNodeOP(' + idx + ',2)">部署监控</button><button data-click="subNodeOP(' + idx + ',6)">部署监控的日志</button></div>' +
							'<button data-click="subNodeOP(' + idx + ',10)">监控情况</button>' +
						'</div>' +
					'</span>';
				ul.appendChild(li);
			});
			if (typeof APPEVEINIT === 'function') {
				APPEVEINIT(ul);
			}
		},
		subNodeOPFun(node,t,app){
			let that = this;
			if(1 === t){
				autolog.success(`${node.out_ip}正在初始化Xrary`)
			}else if(2 === t){
				autolog.success(`${node.out_ip}正在部署监控`)
			}else if(3 === t){
				autolog.success(`${node.out_ip}正在配置Xrary`)
			}else if(4 === t){
				autolog.success(`${node.out_ip}正在配置gost`)
			}else if(5 === t || 6 === t){
				autolog.success(`${node.out_ip}正在获取日志`)
			}
			let data ={
				t:t,
				id:node.id,
				app: app,
				success: function(e) {
					if(1 === t){
						autolog.success(`${node.out_ip}初始化Xrary成功：${e}`,4000)
					}else if(2 === t){
						autolog.success(`${node.out_ip}部署监控成功：${e}`,4000)
					}else if(3 === t){
						autolog.success(`${node.out_ip}配置Xrary成功：${e}`,4000)
					}else if(4 === t){
						autolog.success(`${node.out_ip}配置gost成功：${e}`,4000)
					}else{
						const realStr = e.replace(/\\n/g, '\n').replace(/\\r/g, '\r');
						autolog.confirm(`${node.out_ip}查询详情`,`<pre style="max-height: 400px; overflow: auto; white-space: pre;">${realStr}</pre>`,function (et,txt){
							let data2 ={
								t:t,
								id:node.id,
								app: app,
								success: function(e1) {
									let realStr = e1.replace(/\\n/g, '\n').replace(/\\r/g, '\r');
									txt.innerHTML=`<pre style="max-height: 400px; overflow: auto; white-space: pre;">${realStr}</pre>`;
									autolog.success(`刷新成功`)
								},
								fail: function(e1) {
									autolog.error(`${node.out_ip}获取日志失败：${e1}`,2000)
								}
							}
							devServersNode(data2);
						},true,null,{maxWidth:900,successTit:'刷新'});
					}
				},
				fail: function(e) {
					if(1 === t){
						autolog.error(`${node.out_ip}初始化Xrary失败：${e}`,4000);
					}else if(2 === t){
						autolog.error(`${node.out_ip}部署监控失败：${e}`,4000)
					}else if(3 === t){
						autolog.error(`${node.out_ip}配置Xrary失败：${e}`,4000)
					}else if(4 === t){
						autolog.confirm(`${node.out_ip}配置gost失败`,e,null,false,null,{maxWidth:700})
					}else{
						autolog.error(`${node.out_ip}获取日志失败：${e}`,6000)
					}
				}
			}
			devServersNode(data);
		},
		subNodeOP(idx,t){
			let that = this;
			let node = app.data.nodeList[idx];
			if (!node) return;
			if(3 === t){
				throttle(this.getNodeById(node), 3000)
				return
			}else if(4 === t){
				let html = '';
				for (var index = 0; index < APPTYPENAME.length; index++) {
					html += `<option value="${APPTYPENAME[index]["val"]}">${APPTYPENAME[index]["name"]}（${APPTYPENAME[index]["val"]}）</option>`
				}
				html = `<select id="devServersNode4444">${html}</select>`;
				autolog.confirm(
					`${node.out_ip}正在部署gost,请选择要部署的应用`,
					html,
					success = function(c) {
						let val = document.getElementById("devServersNode4444").value;
						if(val && val!==''){
							throttle(that.getNodeByApp(node,val), 3000);
						}
						c.close();
					},true)
				return
			}
			throttle(this.subNodeOPFun(node,t), 3000)
		},
		getNodeByApp(node,app){
			let that = this;
			let data ={
				app:app,
				success: function(e) {
					let html = '请添加节点，如果已经添加了没有显示，哪请等一会再试。';
					if (e.length >0) {
						html = '';
						e.forEach(function(el) {
							html+=`<div>应用：${app};节点名称：${el.name};出口ip：${el.out_host};入口端口：${el.port};出口端口：${el.server_port};</div>`
						}, this);
					}
					autolog.confirm(
						"请确认你需要部署的节点信息",
						html,
						success = function(c) {
							if (e.length >0)  {
								throttle(that.subNodeOPFun(node,4,app), 3000)
							}
							c.close();
						},true)
				},
				fail: function(e) {
					autolog.error(`获取配置信息失败：${e}`,6000);
				}
			}
			GetServersByApp(data);
		},
		getNodeById(node){
			let that = this;
			let data ={
				id:node.id,
				success: function(e) {
					let html = '请添加节点，如果已经添加了没有显示，哪请等一会再试。';
					if (e.length >0) {
						html = '';
						e.forEach(function(el) {
							html+=`<div>应用：${el.zz_app};节点ID：${el.node_id};节点名称：${el.names};入口ip：${el.in_ip};协议：${el.method};</div>`
						}, this);
					}
					autolog.confirm(
						"请确认你需要部署的节点信息",
						html,
						success = function(c) {
							if (e.length >0)  {
								throttle(that.subNodeOPFun(node,3), 3000)
							}
							c.close();
						},true)
				},
				fail: function(e) {
					autolog.error(`获取配置信息失败：${e}`);
				}
			}
			GetServerNodesById(data);
		},
		toggleDropdown(btn) {
			var dropdown = btn.parentElement;
			var wasOpen = dropdown.classList.contains('open');
			document.querySelectorAll('.action-dropdown.open').forEach(function(el) {
				el.classList.remove('open');
			});
			if (!wasOpen) dropdown.classList.add('open');
		},
		showMonitor(t,idx) {
			var node = app.data.nodeList[idx];
			if (!node) return;
			app.data.trafficMonitorNode = node;
			app.data.trafficMonitorNodeT = t;
			var title = document.getElementById('trafficMonitorTitle');
			var frame = document.getElementById('trafficMonitorFrame');
			if (title) {
				let tit = t===0?'带宽':'流量';
				title.textContent = node.out_ip + ' (' + (node.city || '-') + ') '+tit+'监控';
			}
			if (frame) {
				frame.src = this._buildMonitorUrl(t,node);
			}
			document.getElementById('trafficMonitorModal').classList.add('show');
		},
		_formatMonitorTime(date) {
			var pad = function(n) { return String(n).padStart(2, '0'); };
			return date.getFullYear() + '-' + pad(date.getMonth() + 1) + '-' + pad(date.getDate())
				+ '%20' + pad(date.getHours()) + '%3A' + pad(date.getMinutes()) + '%3A' + pad(date.getSeconds());
		},
		_buildMonitorUrl(t,node) {
			let cmd = 'increase(node_network_receive_bytes_total{ip="' + node.out_ip + '",job="node-monitor",region="' + node.city + '"}[24h])';
			if(t===0){
				cmd = 'irate(node_network_receive_bytes_total{ip="' + node.out_ip + '",job="node-monitor",region="' + node.city + '"}[5m])\n*\n  8';
			}
			var expr = encodeURIComponent(cmd);
			var timeStr = this._formatMonitorTime(new Date());
			return 'http://10.10.25.202:9090/graph?g0.expr=' + expr
				+ '&g0.tab=0&g0.display_mode=lines&g0.show_exemplars=1'
				+ '&g0.range_input=1d'
				+ '&g0.end_input=' + timeStr
				+ '&g0.moment_input=' + timeStr;
		},
		refreshTrafficMonitor() {
			var node = app.data.trafficMonitorNode;
			var frame = document.getElementById('trafficMonitorFrame');
			if (!node || !frame) return;
			frame.src = this._buildMonitorUrl(app.data.trafficMonitorNodeT,node)
		},
		loadNodes() {
			let that = this;
			let data = {
				success: function(list) {
					app.data.nodeList = list;
					that.renderList(list)
				},
				fail: function(e) {
					autolog.error(e);
				}
			}
			getServerNodes(data)
		},
		esc(s) {
			var d = document.createElement('div');
			d.textContent = s || '';
			return d.innerHTML;
		},
		searchUse(e){
			app.data.searchFilter['isUse']=e.target.value;
			this.searchFilterFun();
		},
		searchNodes() {
			let keyword = (document.getElementById('searchInput').value || '').trim().toLowerCase();
			app.data.searchFilter['search']=keyword;
			this.searchFilterFun();
		},
		searchFilterFun(){
			let list = app.data.nodeList || [];
			if (!app.data.searchFilter['search'] && !app.data.searchFilter['isUse']) {
				this.renderList(list);
				return;
			}
			let filtered =list;
			if(app.data.searchFilter['search']){
				let keyword=app.data.searchFilter['search'];
				filtered = filtered.filter(function(node) {
					return (node.names || '').toLowerCase().indexOf(keyword) !== -1
						|| (node.out_ip || '').toLowerCase().indexOf(keyword) !== -1
						|| (node.in_ip || '').toLowerCase().indexOf(keyword) !== -1
						|| (node.city || '').toLowerCase().indexOf(keyword) !== -1
						|| (node.zz_app || '').toLowerCase().indexOf(keyword) !== -1
						|| (node.note || '').toLowerCase().indexOf(keyword) !== -1;
				});
			}
			if (app.data.searchFilter['isUse']){
				let keyword=Number(app.data.searchFilter['isUse']);
				filtered = filtered.filter(function(node) {
					return keyword === node.is_use;
				});
			}
			this.renderList(filtered);
		},
		editNode(idx) {
			let that = this;
			app.data.newNode = app.data.nodeList[idx];
			if (!app.data.newNode) return;
			document.getElementById('modalTitle').textContent = '修改节点';
			that.setEditNodeHtml();
		},
		addNode(){
			let that = this;
			app.data.newNode ={}
			that.setEditNodeHtml();
		},
		setEditNodeHtml(){
			let node = app.data.newNode
			for (const key in app.data.nodeComt) {
			  if (key === 'id') continue;
			
			  const el = document.getElementById(`app-${key}`);
			  if (!el) continue; // 防止报错
			
			  el.value = node[key] ?? ''; // 用 ?? 保留 0 / false
			}
			document.getElementById('editModal').classList.add('show');
		},
		saveNode() {
			let that=this;
			let node = app.data.newNode
			for (const key in app.data.nodeComt) {
			  if (key === 'id') continue;
			  const el = document.getElementById(`app-${key}`);
			  if (!el) continue; // 防止报错
			  node[key] = (el.value ?? '').trim(); // 用 ?? 保留 0 / false
			  if(node[key] === '' && ('out_ip' === key || 'in_ip' === key || 'city' === key)){
				autolog.warn("出口ip和入口ip以及出口国家/地区是必须填的！");
				return;
			  }
			  if('city' === key && !COUNTRIESCN.includes(node[key])){
				autolog.warn("没有此出口国家/地区！");
				return;
			  }
			  if('dw' === key || 'in_ip_port' === key || 'out_ip_port' === key){
				  node[key] = Number(node[key])
			  }
			}
			if(node['out_ip_port'] === 0){
				node['out_ip_port'] = 22;
			}
			if(!node['out_ip_user']){
				node['out_ip_user'] = 'root';
			}
			if(node['in_ip_port'] === 0){
				node['in_ip_port'] = node['out_ip_port'];
			}
			if(node['in_ip_user'] === ''){
				node['in_ip_user'] = node['out_ip_user'];
			}
			if(node['in_ip_pwd'] === ''){
				node['in_ip_pwd'] = node['out_ip_pwd'];
			}
			let data ={
				data:node,
				success: function() {
					that.closeModal('editModal');
					that.loadNodes();
				},
				fail: function(e) {
					autolog.error(e);
				}
			}
			subServersNode(data);
		},
		showDetail(idx) {
			var node = app.data.nodeList[idx];
			if (!node) return;
			window._detailIdx = idx;
			this._renderDetail(node);
			document.getElementById('detailModal').classList.add('show');
		},
		_renderDetail(node) {
			var grid = document.getElementById('detailGrid');
			grid.innerHTML =
				'<span class="label">出口IP</span><span class="value">' + this.esc(node.out_ip) + '</span>' +
				'<span class="label">出口ssh端口</span><span class="value">' + (this.esc(node.out_ip_port) || '-') + '</span>' +
				'<span class="label">出口ssh账号</span><span class="value">' + (this.esc(node.out_ip_user) || '-') + '</span>' +
				'<span class="label">出口ssh密码</span><span class="value">' + (this.esc(node.out_ip_pwd) || '-') + '</span>' +
				'<span class="label">出口国家/地区</span><span class="value">' +node.city + '</span>' +
				'<span class="label">入口IP</span><span class="value">' + this.esc(node.in_ip) + '</span>' +
				'<span class="label">入口ssh端口</span><span class="value">' + (this.esc(node.in_ip_port) || '-') + '</span>' +
				'<span class="label">入口ssh账号</span><span class="value">' + (this.esc(node.in_ip_user) || '-') + '</span>' +
				'<span class="label">入口ssh密码</span><span class="value">' + (this.esc(node.in_ip_pwd) || '-') + '</span>' +
				'<span class="label">带宽</span><span class="value">' + (this.esc(node.dw) || '-') + '</span>'+
				'<span class="label">默认网卡</span><span class="value">' + (this.esc(node.device) || '-') + '</span>'+
				'<span class="label">中转应用</span><span class="value">' + (this.esc(node.zz_app) || '-') + '</span>'+
				'<span class="label">安装Xrary</span><span class="value">' + (1===node.is_xray?'是':'否') + '</span>'+
				'<span class="label">备注</span><span class="value">' + (this.esc(node.note) || '-') + '</span>'+
				'<span class="label">修改时间</span><span class="value">' + node.updated_at + '</span>'+
				'<span class="label">创建时间</span><span class="value">' + node.created_at + '</span>'+
				'<span class="label">节点名称</span><span class="value">' + this.esc(node.names) + '</span>';
		},
		refreshDetail(b) {
			var btn = document.getElementById(b);
			btn.classList.add('spinning');
			setTimeout(function() { btn.classList.remove('spinning'); }, 600);
			this.loadNodes();
			var idx = window._detailIdx;
			if (idx !== undefined && app.data.nodeList[idx]) {
				this._renderDetail(app.data.nodeList[idx]);
			}
		},
		loadMonitor() {
			var btn = document.getElementById('loadMonitorBtn');
			btn.style.background = '#6d28d9';
			btn.textContent = '加载中...';
			btn.disabled = true;
			let that = this;
			let data = {
				success: function(e) {
					btn.style.background = '#7c3aed';
					btn.innerHTML = '<svg width="16" height="16" viewBox="0 0 16 16" fill="none"><rect x="1" y="10" width="3" height="5" rx="1" fill="white"/><rect x="6" y="6" width="3" height="9" rx="1" fill="white"/><rect x="11" y="2" width="3" height="13" rx="1" fill="white"/></svg>加载监控';
					btn.disabled = false;
					autolog.success('监控数据加载成功', 2000);
				},
				fail: function(e) {
					btn.style.background = '#7c3aed';
					btn.innerHTML = '<svg width="16" height="16" viewBox="0 0 16 16" fill="none"><rect x="1" y="10" width="3" height="5" rx="1" fill="white"/><rect x="6" y="6" width="3" height="9" rx="1" fill="white"/><rect x="11" y="2" width="3" height="13" rx="1" fill="white"/></svg>加载监控';
					btn.disabled = false;
					autolog.error('监控数据加载失败：' + e.error, 4000);
				}
			};
			InitNodeMonitor(data);
		},
		closeModal(id) {
			document.getElementById(id).classList.remove('show');
			if (id === 'trafficMonitorModal') {
				var frame = document.getElementById('trafficMonitorFrame');
				if (frame) {
					frame.src = '';
				}
			}
		}
	}
};
