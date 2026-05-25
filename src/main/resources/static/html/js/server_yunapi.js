window.server_yunapi = {
	data: {
		list:[],
		newNode:{},
		nodeComt:{
			key:'',
			secret:'',
			supplier_account:'',
			supplier:'',
			scope:-1,
			cdn:0,
			note:''
		},
	},
	/* 初始化执行 */
	created() {
		const supplier = document.getElementById("app-supplier");
		for (const key in SUPPLIERMAP) {
			supplier.insertAdjacentHTML("beforeend",
				`<option value="${key}">${SUPPLIERMAP[key]}</option>`
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
		supplierText(value) {
			return SUPPLIERMAP[value] || value || '';
		},
		keyCom(s) {
			let r={
				key:'',
				secret:''
			}
			switch (s) {
				case	'ali':
					r={
						key:'AccessKeyId',
						secret:'AccessKeySecret'
					}
					break;
				case	'ucloud':
					r={
						key:'PublicKey',
						secret:'PrivateKey'
					}
					break;
				case	'huawei':
					r={
						key:'AccessKey',
						secret:'SecretKey'
					}
					break;
				case	'cloudflare':
					r={
						key:'APIKey'
					}
					break;
				case	'tencent':
					r={
						key:'SecretId',
						secret:'SecretKey'
					}
					break;
				case	'baidu':
					r={
						key:'AccessKey',
						secret:'SecretKey'
					}
					break;
				case	'qiniu':
					r={
						key:'AccessKey',
						secret:'AccessSecret'
					}
					break;
				case	'jdcloud':
					r={
						key:'Access Key ID',
						secret:'Secret Access Key'
					}
					break;
				case	'namesilo':
					r={
						key:'API Key'
					}
					break;
			}
			return r;
		},
		secretText(value) {
			return SUPPLIERMAP[value] || value || '';
		},
		renderList(nodes) {
			let that = this;
			var ul = document.getElementById('nodeList');
			var header = document.querySelector('.list-header');
			ul.innerHTML = '';
			if (!nodes || nodes.length === 0) {
				header.style.display = 'none';
				ul.innerHTML = '<li style="text-align:center;color:var(--text-gray);padding:40px 0;font-size:14px;">暂无信息，请点击右上角添加</li>';
				return;
			}
			header.style.display = 'grid';
			nodes.forEach(function(node) {
				var idx = app.data.list ? app.data.list.indexOf(node) : 0;
				var li = document.createElement('li');
				li.className = 'node-item';
				let status = node.status===0?'显示':'隐藏';
				let cdn = node.cdn===0?'没开':'开了';
				li.innerHTML =
					'<span class="cell-supplier">' + that.esc(that.supplierText(node.supplier)) + '</span>' +
					'<span class="cell-status">' + that.esc(status) + '</span>' +
					'<span class="cell-status">' + that.esc(cdn) + '</span>' +
					'<span class="cell-supplier">' + that.esc(node.supplier_account) + '</span>' +
					'<span class="cell-note">' + that.esc(node.note) + '</span>' +
					'<span class="action-dropdown">' +
						'<button class="dropdown-toggle" onclick="server_yunapi.methods.toggleDropdown(this)">操作 <svg width="12" height="12" viewBox="0 0 12 12"><path d="M3 5l3 3 3-3" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round"/></svg></button>' +
						'<div class="dropdown-menu">' +
							'<button data-click="editNode(' + idx + ')">修改</button>' +
							'<button data-click="showDetail(' + idx + ')">详情</button>' +
							'<button data-click="delNode(' + node.id + ')">删除</button>' +
						'</div>' +
					'</span>';
				ul.appendChild(li);
			});
			if (typeof APPEVEINIT === 'function') {
				APPEVEINIT(ul);
			}
		},
		toggleDropdown(btn) {
			var dropdown = btn.parentElement;
			var wasOpen = dropdown.classList.contains('open');
			document.querySelectorAll('.action-dropdown.open').forEach(function(el) {
				el.classList.remove('open');
			});
			if (!wasOpen) dropdown.classList.add('open');
		},
		loadNodes() {
			let that = this;
			let data = {
				success: function(list) {
					app.data.list = list;
					that.renderList(list)
				},
				fail: function(e) {
					autolog.error(e.error);
				}
			}
			getServerSupplierApi(data)
		},
		esc(s) {
			var d = document.createElement('div');
			d.textContent = s || '';
			return d.innerHTML;
		},
		changeSupplier(e,t){
			let r=this.keyCom(t ?t: (e?.target?.value || "").trim());
			let key=document.getElementById("form-label-key");
			key.innerHTML=`${r.key}<span style="color: red">*</span>`;
			let formSecret=document.getElementById("form-secret");
			let secret=document.getElementById("form-label-secret");
			if(r.secret){
				formSecret.style.display="block"
				secret.innerHTML=`${r.secret}<span style="color: red">*</span>`;
			}else {
				formSecret.style.display="none"
			}
		},
		searchNodes() {
			let keyword = (document.getElementById('searchInput').value || '').trim().toLowerCase();
			let list = app.data.list || [];
			if (!keyword) {
				this.renderList(list);
				return;
			}
			let filtered = list.filter(function(node) {
				return (node.supplier_account || '').toLowerCase().indexOf(keyword) !== -1
					|| (node.supplier || '').toLowerCase().indexOf(keyword) !== -1
					|| (node.note || '').toLowerCase().indexOf(keyword) !== -1;
			});
			this.renderList(filtered);
		},
		editNode(idx) {
			let that = this;
			app.data.newNode = app.data.list[idx];
			if (!app.data.newNode) return;
			document.getElementById('modalTitle').textContent = '修改';
			this.changeSupplier(null,app.data.newNode['supplier'])
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
		delNode(id){
			let that = this;
			let data ={
				id:id,
				success: function() {
					that.loadNodes();
				},
				fail: function(e) {
					autolog.error(e.error);
				}
			}
			serverSupplierApiDel(data);
		},
		saveNode() {
			let that=this;
			let node = app.data.newNode
			const supplier = document.getElementById(`app-supplier`);
			let r=this.keyCom((supplier.value ?? '').trim());
			for (const key in app.data.nodeComt) {
			  if (key === 'id') continue;
			  const el = document.getElementById(`app-${key}`);
			  if (!el) continue; // 防止报错
			  let val = (el?.value ?? '').trim();
			  node[key] = key==='scope' || key==='cdn'?Number(val):val;
			  if(node[key] === ''){
				  if('secret' !== key ){
					  autolog.warn("除了(AccessKeySecret/私钥)其它都是必须填的！");
					  return;
				  }else if (r.secret){
                      autolog.warn('此供应商(AccessKeySecret/私钥)也是必填的');
                      return;
				  }
			  }
			}
			if(!r.secret){
				node['secret']='';
			}
			let data ={
				data:node,
				success: function() {
					that.closeModal('editModal');
					that.loadNodes();
				},
				fail(e) {
					autolog.error(e.error);
				}
			}
			postServerSupplierApi(data);
		},
		showDetail(idx) {
			var node = app.data.list[idx];
			if (!node) return;
			window._detailIdx = idx;
			this._renderDetail(node);
			document.getElementById('detailModal').classList.add('show');
		},
		_renderDetail(node) {
			var grid = document.getElementById('detailGrid');
			let r =this.keyCom(node.supplier);
			let html =
				'<span class="label">名称</span><span class="value">' + this.esc(node.note) + '</span>' +
				'<span class="label">CDN流量套餐</span><span class="value">' + (0===node.scope?'全球，不包含中国内地':1===node.scope?'全球可用':2===node.scope?'仅中国内地可用':'未购买套餐(一般是按量计费)') + '</span>' +
				'<span class="label">供应商</span><span class="value">' + (this.esc(this.supplierText(node.supplier)) || '-') + '</span>' +
				'<span class="label">供应商账号</span><span class="value">' + (this.esc(node.supplier_account) || '-') + '</span>' +
				'<span class="label">'+r.key+'</span><span class="value">' + (this.esc(node.key) || '-') + '</span>';
			html+=r.secret?'<span class="label">'+r.secret+'</span><span class="value">' + (this.esc(node.secret) || '-') + '</span>':'';
			grid.innerHTML =html;
		},
		refreshDetail() {
			var btn = document.getElementById('detailRefreshBtn');
			btn.classList.add('spinning');
			setTimeout(function() { btn.classList.remove('spinning'); }, 600);
			this.loadNodes();
			var idx = window._detailIdx;
			if (idx !== undefined && app.data.list[idx]) {
				this._renderDetail(app.data.list[idx]);
			}
		},
		closeModal(id) {
			document.getElementById(id).classList.remove('show');
		}
	}
};
