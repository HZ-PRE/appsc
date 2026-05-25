window.server_url = {
	data: {
		list:[],
		newNode:{},
		nodeComt:{
			url:'',
			note:''
		},
	},
	/* 初始化执行 */
	created() {
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
				ul.innerHTML = '<li style="text-align:center;color:var(--text-gray);padding:40px 0;font-size:14px;">暂无信息，请点击右上角添加</li>';
				return;
			}
			header.style.display = 'grid';
			nodes.forEach(function(node) {
				var idx = app.data.list ? app.data.list.indexOf(node) : 0;
				var li = document.createElement('li');
				li.className = 'node-item';
				li.innerHTML =
					'<span class="cell-note">' + that.esc(node.note) + '</span>' +
					'<span class="cell-url">' + that.esc(node.url) + '</span>' +
					'<span class="action-dropdown">' +
						'<button class="dropdown-toggle" onclick="server_url.methods.toggleDropdown(this)">操作 <svg width="12" height="12" viewBox="0 0 12 12"><path d="M3 5l3 3 3-3" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round"/></svg></button>' +
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
					autolog.error(e);
				}
			}
			getServerUrl(data)
		},
		esc(s) {
			var d = document.createElement('div');
			d.textContent = s || '';
			return d.innerHTML;
		},
		searchNodes() {
			let keyword = (document.getElementById('searchInput').value || '').trim().toLowerCase();
			let list = app.data.list || [];
			if (!keyword) {
				this.renderList(list);
				return;
			}
			let filtered = list.filter(function(node) {
				return (node.url || '').toLowerCase().indexOf(keyword) !== -1
					|| (node.note || '').toLowerCase().indexOf(keyword) !== -1;
			});
			this.renderList(filtered);
		},
		editNode(idx) {
			let that = this;
			app.data.newNode = app.data.list[idx];
			if (!app.data.newNode) return;
			document.getElementById('modalTitle').textContent = '修改';
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
					autolog.error(e);
				}
			}
			serverUrlDel(data);
		},
		saveNode() {
			let that=this;
			let node = app.data.newNode
			for (const key in app.data.nodeComt) {
			  if (key === 'id') continue;
			  const el = document.getElementById(`app-${key}`);
			  if (!el) continue; // 防止报错
			  node[key] = (el.value ?? '').trim(); // 用 ?? 保留 0 / false
			  if(node[key] === '' && ('url' === key || 'note' === key)){
				autolog.warn("URL和内容是必须填的！");
				return;
			  }
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
			createServersUrl(data);
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
			grid.innerHTML =
				'<span class="label">内容</span><span class="value">' + this.esc(node.note) + '</span>' +
				'<span class="label">URL</span><span class="value">' + (this.esc(node.url) || '-') + '</span>'
		},
		refreshDetail(b) {
			var btn = document.getElementById(b);
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
