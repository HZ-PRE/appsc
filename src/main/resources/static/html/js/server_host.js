window.server_host = {
	data: {
		list: [],
		filteredList: [],
		currentHost: {},
		detailId: null,
		host:'',
		hostFields: {
			domain: '',
			supplier: '',
			supplier_account: '',
			status: 0,
			note: '',
			supplier_id:0,
			scope:0,
			beian:0,
			app:''
		},
		apps:[]
	},

	created() {
		const appType = document.getElementById("app-app");
		app.data.apps = [...APPTYPENAME,...APPKFTYPE]
		for (var index = 0; index < app.data.apps.length; index++) {
			appType.insertAdjacentHTML("beforeend",
				`<option value="${app.data.apps[index]['val']}"  key="${index}">${app.data.apps[index]["name"]}</option>`
			);
		}
		const supplier = document.getElementById("app-supplier");
		for (const key in SUPPLIERMAP) {
			supplier.insertAdjacentHTML("beforeend",
				`<option value="${key}">${SUPPLIERMAP[key]}</option>`
			);
		}
		if (!window.__serverHostDropdownListener) {
			window.__serverHostDropdownListener = true;
			document.addEventListener('click', function (event) {
				if (!event.target.closest('.action-dropdown')) {
					document.querySelectorAll('.action-dropdown.open').forEach(function (el) {
						el.classList.remove('open');
					});
				}
			});
		}
		this.methods.loadHosts();
	},

	methods: {
		loadHosts() {
			const that = this;
			getServerHost({
				success(list) {
					app.data.list = Array.isArray(list) ? list : [];
					that.searchHosts();
				},
				fail(e) {
					autolog.error(e.error);
				}
			});
		},
		getSupplierApis(supplier,is=true){
			if (!supplier){
				return;
			}
			let that=this;
			const su = document.getElementById("app-supplier_id");
			let cdn =1===app.data.currentHost['is_self']?1:0
			su.innerHTML='<option value="">请选择(可不选)</option>'
			let data = {
				cdn:cdn,
				supplier: supplier,
				success: function(list) {
					for (const key in list) {
						su.insertAdjacentHTML("beforeend",
							`<option value=${list[key].id} title="${list[key]['supplier_account']}" data-scope="${list[key]['scope']}">(${that.supplierText(list[key].supplier)})${list[key].note}</option>`
						);
					}
					if (is){
						that.fillForm(app.data.currentHost);
						that.openModal('editModal');
					}
				},
				fail: function(e) {
					autolog.error(e.error);
				}
			}
			getServerSupplierApiBySupplier(data)
		},
		renderList(hosts, flatMode) {
			const ul = document.getElementById('hostList');
			const header = document.getElementById('hostTableHead');
			if (!ul || !header) return;

			ul.innerHTML = '';
			if (!hosts || hosts.length === 0) {
				header.style.display = 'none';
				ul.innerHTML = '<li class="empty-state">暂无域名，请点击右上角添加</li>';
				return;
			}

			header.style.display = 'grid';
			const fragment = document.createDocumentFragment();
			const childrenByParent = this.groupChildren(hosts);
			const parentHosts = hosts.filter((host) => this.parentId(host) === 0);
			parentHosts.forEach((host) => {
				const id = this.hostId(host);
				const children = childrenByParent[id] || [];
				let isChildShow = flatMode || (app.data.currentHost['parent_id']!==undefined && id===app.data.currentHost['parent_id']);
				fragment.appendChild(this.createHostRow(host, {
					isChild: false,
					isHidden: false,
					hasChildren: children.length > 0
				},host));
				children.forEach((child) => {
					fragment.appendChild(this.createHostRow(child, {
						isChild: true,
						isHidden: !isChildShow,
						hasChildren: false
					},host));
				});
			});

			ul.appendChild(fragment);
			if (typeof APPEVEINIT === 'function') {
				APPEVEINIT(ul);
			}
		},

		groupChildren(hosts) {
			return hosts.reduce((map, host) => {
				const parentId = this.parentId(host);
				if (parentId !== 0) {
					if (!map[parentId]) map[parentId] = [];
					map[parentId].push(host);
				}
				return map;
			}, {});
		},

		createHostRow(host, options,parent={}) {
			const id = this.hostId(host);
			const parentId = this.parentId(host);
			const hasChildren = Boolean(options?.hasChildren);
			const isChild = Boolean(options?.isChild);
			const isHidden = Boolean(options?.isHidden);
			const rowClick = hasChildren ? 'toggleChildren(' + id + ')' : 'showDetail(' + id + ')';
			const domainPrefix = isChild ? '<span class="tree-spacer"></span>' : (hasChildren ? '<span class="tree-toggle">+</span>' : '<span class="tree-spacer"></span>');
			const app = host.app?'('+host.app+')':'';
			const li = document.createElement('li');
			li.dataset.hostId = String(id);
			li.dataset.parentId = String(parentId);
			li.className = 'host-row' + (isChild ? ' is-child' : '') + (isHidden ? ' is-hidden' : '');
			let isEdit=!host.is_self?true:AdminAppAccessKey?true:false;
			li.innerHTML =
				'<span data-click="' + rowClick + '" class="cell-domain" title="' + this.esc(host.domain) + '">' + domainPrefix + this.esc(host.domain) + '</span>' +
				'<span data-click="' + rowClick + '" class="cell-muted" title="' + this.esc(this.supplierText(host.supplier)) + '">' + this.esc(this.supplierText(host.supplier)+app) + '</span>' +
				'<span data-click="' + rowClick + '" class="cell-muted" title="' + this.esc(host.supplier_account) + '">' + this.esc(host.supplier_account) + '</span>' +
				'<span data-click="' + rowClick + '" class="cell-muted" title="' + this.esc(this.statusText(host.status))+'('+this.esc(this.isUseText(host.is_use)) + ')">' + this.esc(this.statusText(host.status))+'('+this.esc(this.isUseText(host.is_use)) + ')</span>' +
				'<span data-click="' + rowClick + '" class="cell-note" title="' + this.esc(host.note) + '">' + (this.esc(host.note) || '-') + '</span>' +
				'<span data-click="' + rowClick + '" class="cell-time" title="' + this.esc(host.created_at) + '">' + (this.esc(host.created_at) || '-') + '</span>' +
				'<span class="action-dropdown" id="'+`action-dropdown${id}`+'">' +
					'<button class="dropdown-toggle" data-click="toggleDropdown(event)">操作 <svg width="12" height="12" viewBox="0 0 12 12"><path d="M3 5l3 3 3-3" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round"/></svg></button>' +
					'<div class="dropdown-menu">' +
				(isChild?'':'<button data-click="addHost(' + id + ",'"+this.esc(parent.domain)+"',0,"+this.esc(parent.beian)+')">普通添加</button>') +
				(isChild?'':'<button data-click="addHost(' + id + ",'"+this.esc(parent.domain)+"',1,"+this.esc(parent.beian)+')">生成式添加</button>') +
						'<button data-click="showDetail(' + id + ')">详情</button>' +
				(isEdit?'<button data-click="editHost(' + id + ",'"+this.esc(parent.domain)+"',"+this.esc(parent.beian)+')">修改</button><button class="btn-danger" data-click="deleteHost(' + id + ')">删除</button>':'') +
					'</div>' +
				'</span>';
			return li;
		},

		toggleChildren(id) {
			const parentId = Number(id) || 0;
			if (!parentId) return;
			document.querySelectorAll('[data-parent-id="' + parentId + '"]').forEach(function (row) {
				row.classList.toggle('is-hidden');
			});
		},

		showChile(id) {
			this.toggleChildren(id);
		},

		searchHosts() {
			const keyword = (document.getElementById('searchInput')?.value || '').trim().toLowerCase();
			const statusFilter = document.getElementById('statusFilter')?.value ?? '';
			const useFilter = document.getElementById('useFilter')?.value ?? '';
			const list = app.data.list || [];
			const filtered = list.filter((host) => {
				return this.hostMatches(host, keyword) && this.hostFilterMatches(host, statusFilter, useFilter);
			});
			app.data.filteredList = filtered;
			this.renderList(filtered, Boolean(keyword || statusFilter || useFilter));
		},

		hostMatches(host, keyword) {
			if (!keyword) return true;
			return [
				host?.domain,
				host?.supplier,
				this.supplierText(host?.supplier),
				host?.supplier_account,
				this.statusText(host?.status),
				this.isUseText(host?.is_use),
				host?.note,
				host?.created_at
			].some((value) => String(value || '').toLowerCase().includes(keyword)) || (host?.parent_id===0 && [host?.domain].some((value) => String(value || '').toLowerCase().includes(getRootDomain(keyword))));
		},

		hostFilterMatches(host, statusFilter, useFilter) {
			const statusOk = statusFilter === '' || Number(host?.status) === Number(statusFilter);
			const useOk = useFilter === '' || Number(host?.is_use) === Number(useFilter);
			return statusOk && useOk;
		},
		changeApp(e){
			if(!e || !e.target.value){
				return
			}
			let app1 =app.data.apps[e?.target?.options?.selectedIndex-1]
			app.data.currentHost['origin_domain']=app1['origin']
		},
		changeSupplier(e){
			this.getSupplierApis(e?.target?.value?.trim() || "",false);
		},
		changeSupplierId(e){
			if(!e || !e.target.value){
				return
			}
			let appSupplierAccount=document.getElementById("app-supplier_account");
			if(e.target.selectedOptions[0]?.title){
				appSupplierAccount.value=e.target.selectedOptions[0]?.title;
			}
			if(e.target.selectedOptions[0]?.dataset?.scope){
				let scope=Number(e.target.selectedOptions[0]?.dataset?.scope) ||0
				autolog.warn('此账号流量套餐只含：'+(scope===0?'全球，不包含中国内地':scope===1?'全球可用':scope===2?'仅中国内地可用':'未购买流量套餐一般是按量计费'+'，请谨慎选择加速套餐，以免格外扣费'),6000)
			}
		},
		setScopeHtml(t,is_self=0){
			let appScope=document.getElementById('app-scope');
			let h='';
			if (0===is_self){
				h='<option value="-1">不加速</option>';
			}
			h+='<option value="0">全球，不包含中国内地</option>';
			if(1===t){
				h+=`<option value="1">全球(需备案)</option><option value="2">仅中国内地(需备案)</option>`
			}
			appScope.innerHTML = h;
		},
		addHost(parentId=0,domain='',is_self=0,beian=0) {
			app.data.currentHost = {
				parent_id: Number(parentId),
				domain: domain,
				status: 0,
				supplier_id: 0,
				is_self:is_self,
				origin_domain:'',
				scope:0,
				beian:beian
			};
			let appBeian=document.getElementById('app-beian');
			appBeian.value=beian
			if(0===parentId){
				appBeian.removeAttribute("disabled")
				this.setScopeHtml(0,is_self)
			}else {
				appBeian.disabled = true;
				this.setScopeHtml(beian,is_self)
			}
			app.data.host = domain;
			document.getElementById('modalTitle').textContent = '添加域名';
			this.fillForm(app.data.currentHost);
			this.openModal('editModal');
		},

		editHost(id=0,domain='',beian=0) {
			const host = this.findHost(id);
			if (!host) return;
			host['beian']=beian;
			app.data.host = domain;
			app.data.currentHost = { ...host };
			this.getSupplierApis(app.data.currentHost['supplier'])
			let appBeian=document.getElementById('app-beian');
			appBeian.value=beian
			if(0===host['parent_id']){
				appBeian.removeAttribute("disabled")
			}else {
				appBeian.disabled = true;
			}
			this.setScopeHtml(beian)
			document.getElementById('modalTitle').textContent = '修改域名';
		},

		fillForm(host) {
			Object.keys(app.data.hostFields).forEach((key) => {
				const el = document.getElementById('app-' + key);
				if (el) {
					el.value = String(host?.[key] ?? app.data.hostFields[key] ?? '');
				}
			});
		},

		readForm() {
			const host = { ...(app.data.currentHost || {}) };
			Object.keys(app.data.hostFields).forEach((key) => {
				const el = document.getElementById('app-' + key);
				const value = (el?.value ?? '').trim();
				host[key] = key === 'status'? Number(value) : value;
			});
			host.parent_id = Number(host.parent_id) || 0;
			host.scope = Number(host.scope) || 0;
			host.beian = Number(host.beian) || 0;
			host.supplier_id = Number(host.supplier_id) || 0;
			return host;
		},

		validateHost(host) {
			if (host.parent_id>0 && (!host.domain || !host.domain.endsWith("."+app.data.host))) {
				autolog.warn('请正确填写域名');
				return false;
			}
			if (!host.supplier) {
				autolog.warn('请填写供应商');
				return false;
			}
			if (!host.supplier_account) {
				autolog.warn('请填写供应商账号');
				return false;
			}
			if (1===host.is_self && host.supplier_id  === 0) {
				autolog.warn('请选择授权API');
				return false;
			}
			if (1===host.is_self && 0>host.scope) {
				autolog.warn('必须选择加速范围');
				return false;
			}
			if (0 < host.scope && 1 != host.beian) {
				autolog.warn('此域名未备案，请先备案');
				return false;
			}
			return true;
		},
		saveHost(){
			const that = this;
			const host = this.readForm();
			if (1===host['is_self'] && !host['id']){
				autolog.confirm("请核实信息",'请核实信息，确认无误后，再提交申请，后续无法修改！',
					function (e) {
						e.close();
						throttle(that.postSaveHost(host),60000)
					},true,null,{successTit:'提交申请',failTit:'再想想'});
			}
		},
		postSaveHost(host) {
			const that = this;
			if (!this.validateHost(host)) return;
			let fun;
			if (1===host['is_self'] && !host['id']){
				fun=autolog.warn("请等待，没有结果前不可重复操作",-1)
			}
			createServersHost({
				data: host,
				success() {
					if (typeof fun === 'function') {
						fun()
					}
					autolog.success("保存成功")
					that.closeModal('editModal');
					that.loadHosts();
				},
				fail(e) {
					if (typeof fun === 'function') {
						fun()
					}
					if(e.error.startsWith("此错误可忽略")){
						autolog.success("保存成功")
						that.closeModal('editModal');
						that.loadHosts();
					}
					autolog.confirm("报错了",`${e.error}`);
				}
			});
		},

		deleteHost(id) {
			if (!id) return;
			const that = this;
			serverHostDel({
				id,
				success() {
					autolog.success("删除成功")
					that.loadHosts();
				},
				fail(e) {
					autolog.error(e.error);
				}
			});
		},

		showDetail(id) {
			const host = this.findHost(id);
			if (!host) return;
			app.data.detailId = id;
			this.renderDetail(host);
			this.openModal('detailModal');
		},

		renderDetail(host) {
			const grid = document.getElementById('detailGrid');
			const parent = this.findHost(this.parentId(host));
			if (!grid) return;
			grid.innerHTML =
				'<span class="label">域名</span><span class="value">' + (this.esc(host.domain) || '-') + '</span>' +
				'<span class="label">父级域名</span><span class="value">' + (this.esc(parent?.domain) || '-') + '</span>' +
				'<span class="label">供应商</span><span class="value">' + (this.esc(this.supplierText(host.supplier)) || '-') + '</span>' +
				'<span class="label">供应商账号</span><span class="value">' + (this.esc(host.supplier_account) || '-') + '</span>' +
				'<span class="label">状态</span><span class="value">' + this.esc(this.statusText(host.status)) + '</span>' +
				'<span class="label">使用</span><span class="value">' + this.esc(this.isUseText(host.is_use)) + '</span>' +
				'<span class="label">是否自建</span><span class="value">' + this.esc(this.isSelfText(host.is_self)) + '</span>' +
				'<span class="label">是否备案</span><span class="value">' + (1===host.beian?"已备案":"未备案") + '</span>' +
				'<span class="label">加速范围</span><span class="value">' + (0===host.scope?'全球，不包含中国内地':1===host.scope?'全球可用':2===host.scope?'仅中国内地可用':'未购买流量套餐(一般是按量计费)') + '</span>' +
				'<span class="label">应用</span><span class="value">' + (this.esc(host?.app) || '-') + '</span>' +
				'<span class="label">备注</span><span class="value">' + (this.esc(host.note) || '-') + '</span>' +
				'<span class="label">创建时间</span><span class="value">' + (this.esc(host.created_at) || '-') + '</span>' +
				'<span class="label">源站域名</span><span class="value">' + (this.esc(host.origin_domain) || '-') + '</span>';
		},

		refreshDetail() {
			const id = app.data.detailId;
			const that = this;
			getServerHost({
				success(list) {
					app.data.list = Array.isArray(list) ? list : [];
					that.searchHosts();
					const host = that.findHost(id);
					if (host) that.renderDetail(host);
				},
				fail(e) {
					autolog.error(e.error);
				}
			});
		},

		findHost(id) {
			return (app.data.list || []).find((host) => this.hostId(host) === Number(id));
		},

		hostId(host) {
			return Number(host?.id) || 0;
		},

		parentId(host) {
			return Number(host?.parent_id) || 0;
		},

		statusText(status) {
			return Number(status) === 0 ? '开启' : '关闭';
		},
		isUseText(isUse) {
			return Number(isUse) === 0 ? '未使用' : '已使用';
		},
		isSelfText(isSelf) {
			return Number(isSelf) === 0 ? '手动创建建' : '此域名为系统生成';
		},
		supplierText(value) {
			return SUPPLIERMAP[value] || value || '';
		},

		toggleDropdown(event) {
			const dropdown = event?.target?.closest?.('.action-dropdown');
			if (!dropdown) return;
			const wasOpen = dropdown.classList.contains('open');
			document.querySelectorAll('.action-dropdown.open').forEach(function (el) {
				el.classList.remove('open');
			});
			if (!wasOpen) {
				dropdown.classList.add('open');
			}
		},

		openModal(id) {
			document.getElementById(id)?.classList.add('show');
		},

		closeModal(id) {
			document.getElementById(id)?.classList.remove('show');
		},

		esc(value) {
			const div = document.createElement('div');
			div.textContent = value ?? '';
			return div.innerHTML;
		}
	}
};
