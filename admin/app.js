const API = (window.HAODE_ADMIN_CONFIG && window.HAODE_ADMIN_CONFIG.apiBaseUrl)
  || 'http://127.0.0.1:8080/api';
let token = sessionStorage.getItem('haode_admin_token') || '';
let products = [];
let reviews = [];
let draftTypes = [];

const $ = (selector) => document.querySelector(selector);

function productImageSrc(item) {
  const url = (item && (item.imageUploadUrl || item.imageUrl)) || '';
  if (!url) return '';
  if (/^https?:\/\//.test(url)) return url;
  return API.replace(/\/api$/, '') + (url.startsWith('/') ? url : `/${url}`);
}

function showImagePreview(url) {
  const preview = $('#product-image-preview');
  if (!url) {
    preview.classList.add('hidden');
    preview.removeAttribute('src');
    return;
  }
  preview.src = url;
  preview.classList.remove('hidden');
}

function emptyType() {
  return { id: '', name: '', intro: '', imageUrl: '', imageUploadUrl: '', file: null, preview: '', saving: false };
}

function currentProductId() {
  return ($('#product-form').elements.id.value || '').trim();
}

function setTypeHint() {
  const hint = $('#types-hint');
  if (!hint) return;
  hint.textContent = currentProductId()
    ? '每个颜色填完后点右侧「提交此类型」，不会离开当前页。'
    : '先点「保存商品」，再给每个颜色单独点「提交此类型」。';
}

function typeFromCard(card) {
  return {
    name: (card.querySelector('input[data-k="name"]').value || '').trim(),
    intro: (card.querySelector('input[data-k="intro"]').value || '').trim(),
    file: card.querySelector('input[data-k="file"]').files[0] || null,
  };
}

function renderTypes() {
  const wrap = $('#product-types');
  if (!wrap) return;
  const locked = !currentProductId();
  wrap.innerHTML = draftTypes.map((item, index) => `
    <article class="type-card" data-index="${index}" data-id="${escapeHtml(item.id || '')}">
      <label>类型名称<input data-k="name" value="${escapeHtml(item.name)}" placeholder="红色" ${locked ? 'disabled' : ''}></label>
      <label>类型简介<input data-k="intro" value="${escapeHtml(item.intro)}" placeholder="春日暖红 · 独立图文" ${locked ? 'disabled' : ''}></label>
      <div class="type-media">
        <label>类型图片<input data-k="file" type="file" accept="image/jpeg,image/png,image/gif,image/webp" ${locked ? 'disabled' : ''}></label>
        ${item.preview ? `<img class="preview" src="${escapeHtml(item.preview)}" alt="">` : ''}
      </div>
      <div class="type-actions">
        <button type="button" class="primary save-type" ${locked ? 'disabled' : ''}>提交此类型</button>
        <button type="button" class="secondary remove-type">删除</button>
      </div>
    </article>
  `).join('') || '<p class="muted">还没有类型。保存商品后可添加红 / 蓝 / 粉 / 黄。</p>';
  wrap.querySelectorAll('input[data-k="file"]').forEach((input) => {
    input.onchange = (event) => {
      const card = input.closest('.type-card');
      const file = event.target.files && event.target.files[0];
      if (!file) return;
      const url = URL.createObjectURL(file);
      let preview = card.querySelector('img.preview');
      if (!preview) {
        preview = document.createElement('img');
        preview.className = 'preview';
        preview.alt = '';
        card.querySelector('.type-media').appendChild(preview);
      }
      preview.src = url;
    };
  });
  wrap.querySelectorAll('.save-type').forEach((button) => {
    button.onclick = () => saveType(button.closest('.type-card'));
  });
  wrap.querySelectorAll('.remove-type').forEach((button) => {
    button.onclick = () => removeType(button.closest('.type-card'));
  });
  setTypeHint();
}

async function saveType(card) {
  const error = $('#type-error');
  if (error) error.textContent = '';
  const productId = currentProductId();
  if (!productId) {
    if (error) error.textContent = '请先保存商品，再提交类型';
    return;
  }
  const fields = typeFromCard(card);
  if (!fields.name) {
    if (error) error.textContent = '请填写类型名称，例如红色';
    return;
  }
  const index = Number(card.dataset.index);
  const typeId = card.dataset.id;
  try {
    const saved = await request(
      typeId ? `/admin/products/${productId}/types/${typeId}` : `/admin/products/${productId}/types`,
      {
        method: typeId ? 'PUT' : 'POST',
        body: JSON.stringify({ name: fields.name, intro: fields.intro, active: true, sortOrder: index }),
      }
    );
    draftTypes[index] = {
      ...emptyType(),
      ...saved,
      file: null,
      preview: productImageSrc(saved),
    };
    card.dataset.id = saved.id;
    if (fields.file) {
      const body = new FormData();
      body.append('file', fields.file);
      const withImage = await request(`/admin/products/${productId}/types/${saved.id}/image`, { method: 'POST', body });
      const updated = (withImage.types || []).find((item) => item.id === saved.id) || saved;
      draftTypes[index] = { ...emptyType(), ...updated, preview: productImageSrc(updated) };
    }
    if (error) error.textContent = '';
    renderTypes();
    const product = products.find((item) => item.id === productId);
    if (product) product.types = draftTypes.filter((item) => item.id).map((item) => ({ ...item }));
  } catch (err) {
    if (error) error.textContent = err.message || '类型保存失败';
  }
}

async function removeType(card) {
  const productId = currentProductId();
  const typeId = card.dataset.id;
  const index = Number(card.dataset.index);
  try {
    if (productId && typeId) {
      await request(`/admin/products/${productId}/types/${typeId}`, { method: 'DELETE' });
    }
    draftTypes.splice(index, 1);
    renderTypes();
  } catch (err) {
    const error = $('#type-error');
    if (error) error.textContent = err.message || '删除失败';
  }
}
const escapeHtml = (value) => String(value ?? '').replace(/[&<>"']/g, (char) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[char]));

async function request(path, options = {}) {
  const headers = { ...(token ? { Authorization: `Bearer ${token}` } : {}), ...(options.headers || {}) };
  if (!(options.body instanceof FormData)) headers['Content-Type'] = 'application/json';
  let response;
  try {
    response = await fetch(`${API}${path}`, { ...options, headers });
  } catch (error) {
    throw new Error(`无法连接管理 API：${API}。请确认 8080 网关已启动。`);
  }
  if (!response.ok) throw new Error(await readError(response));
  if (response.status === 204 || response.status === 205) return null;
  const text = await response.text();
  if (!text) return null;
  return JSON.parse(text);
}

async function readError(response) {
  const text = await response.text();
  try {
    const body = JSON.parse(text);
    return body.message || body.error || text || `请求失败：HTTP ${response.status}`;
  } catch (error) {
    return text || `请求失败：HTTP ${response.status}`;
  }
}

function showSection(name) {
  document.querySelectorAll('.section').forEach((section) => section.classList.toggle('active-section', section.id === name));
  document.querySelectorAll('nav button').forEach((button) => button.classList.toggle('active', button.dataset.section === name));
  $('#page-title').textContent = ({ overview: '经营总览', homepage: '首页展示', products: '商品管理', users: '用户信息', orders: '订单管理', 'after-sales': '售后退款', coupons: '优惠券', announcements: '公告', reviews: '评价审核', support: '客服中心' })[name];
  if (name === 'overview') loadSummary();
  if (name === 'homepage') loadHomepage();
  if (name === 'products') loadProducts();
  if (name === 'users') loadUsers();
  if (name === 'orders') loadOrders();
  if (name === 'after-sales') loadAfterSales();
  if (name === 'coupons') loadCoupons();
  if (name === 'announcements') loadAnnouncements();
  if (name === 'reviews') loadReviews();
  if (name === 'support') startSupport();
  else stopSupport();
}

async function loadSummary() { const data = await request('/admin/summary'); $('#metric-users').textContent = data.userCount; $('#metric-orders').textContent = data.orderCount; $('#metric-paid').textContent = `¥${Number(data.paidAmount).toFixed(2)}`; }
async function loadHomepage() {
  products = await request('/admin/products');
  const onHome = products.filter((item) => item.homepage);
  $('#homepage-table').innerHTML = `<table><thead><tr><th>图片</th><th>商品</th><th>区域</th><th>首页</th><th>操作</th></tr></thead><tbody>${products.map((item) => `<tr><td>${item.imageUrl || item.imageUploadUrl ? `<img class="thumb" src="${escapeHtml(productImageSrc(item))}" alt="">` : '无图片'}</td><td>${escapeHtml(item.name)}<br><small>${escapeHtml(item.subtitle)}</small></td><td>${escapeHtml(item.zone || '未分区')}</td><td>${item.homepage ? '展示中' : '未上首页'}</td><td><button class="secondary toggle-home" data-id="${item.id}" data-on="${item.homepage ? '1' : '0'}">${item.homepage ? '移出首页' : '上首页'}</button></td></tr>`).join('')}</tbody></table>${onHome.length ? `<p class="muted">当前首页 ${onHome.length} 件</p>` : '<p class="muted">还没有商品上首页，小程序首页会是空的。</p>'}`;
  document.querySelectorAll('.toggle-home').forEach((button) => {
    button.onclick = () => toggleHomepage(button.dataset.id, button.dataset.on !== '1');
  });
}

async function toggleHomepage(id, homepage) {
  const product = products.find((item) => item.id === id);
  if (!product) return;
  await request(`/admin/products/${id}`, {
    method: 'PUT',
    body: JSON.stringify({
      name: product.name,
      subtitle: product.subtitle,
      price: product.price,
      originalPrice: product.originalPrice,
      color: product.color,
      accent: product.accent,
      pattern: product.pattern,
      tag: product.tag,
      zone: product.zone || '床品区',
      homepage,
      sortOrder: product.sortOrder || 0,
      imageUrl: product.imageUrl,
      active: product.active,
    }),
  });
  const section = document.querySelector('.section.active-section');
  if (section && section.id === 'homepage') loadHomepage();
  else loadProducts();
}

async function loadProducts() {
  products = await request('/admin/products');
  fillZoneFilter();
  renderProductTable();
}

function fillZoneFilter() {
  const select = $('#products-zone');
  if (!select) return;
  const current = select.value;
  const zones = [...new Set(products.map((item) => item.zone || '未分区'))];
  select.innerHTML = `<option value="">全部区域</option>${zones.map((zone) => `<option value="${escapeHtml(zone)}">${escapeHtml(zone)}</option>`).join('')}`;
  if (zones.includes(current)) select.value = current;
}

function filteredProducts() {
  const query = (($('#products-q') && $('#products-q').value) || '').trim().toLowerCase();
  const zone = ($('#products-zone') && $('#products-zone').value) || '';
  const status = ($('#products-status') && $('#products-status').value) || '';
  return products.filter((item) => {
    if (zone && (item.zone || '未分区') !== zone) return false;
    if (status === 'active' && !item.active) return false;
    if (status === 'inactive' && item.active) return false;
    if (!query) return true;
    const hay = [
      item.name,
      item.subtitle,
      item.tag,
      item.zone,
      ...(item.types || []).map((type) => `${type.name || ''} ${type.intro || ''}`),
    ].join(' ').toLowerCase();
    return hay.includes(query);
  });
}

function renderProductTable() {
  const list = filteredProducts();
  $('#products-table').innerHTML = `<table><thead><tr><th>图片</th><th>商品</th><th>类型</th><th>区域</th><th>首页</th><th>售价</th><th>状态</th><th>操作</th></tr></thead><tbody>${list.map((item) => `<tr><td>${item.imageUrl || item.imageUploadUrl ? `<img class="thumb" src="${escapeHtml(productImageSrc(item))}" alt="">` : '无图片'}</td><td>${escapeHtml(item.name)}<br><small>${escapeHtml(item.subtitle)}</small></td><td>${escapeHtml((item.types || []).map((type) => type.name).join(' / ') || '默认款')}</td><td>${escapeHtml(item.zone || '未分区')}</td><td>${item.homepage ? '是' : '否'}</td><td>¥${Number(item.price).toFixed(2)}</td><td>${item.active ? '在售' : '已下架'}</td><td><button class="secondary edit-product" data-id="${item.id}">编辑</button> <button class="secondary toggle-home" data-id="${item.id}" data-on="${item.homepage ? '1' : '0'}">${item.homepage ? '移出首页' : '上首页'}</button> ${item.active ? `<button class="secondary delete-product" data-id="${item.id}">下架</button>` : ''}</td></tr>`).join('') || '<tr><td colspan="8">没有匹配的商品</td></tr>'}</tbody></table>`;
  document.querySelectorAll('.edit-product').forEach((button) => button.onclick = () => editProduct(button.dataset.id));
  document.querySelectorAll('.delete-product').forEach((button) => button.onclick = () => deleteProduct(button.dataset.id));
  document.querySelectorAll('#products-table .toggle-home').forEach((button) => button.onclick = () => toggleHomepage(button.dataset.id, button.dataset.on !== '1'));
}

function setEditorOpen(open) {
  const wrap = $('#product-form-wrap');
  const toggle = $('#toggle-editor');
  wrap.classList.toggle('collapsed', !open);
  if (toggle) toggle.textContent = open ? '收起' : '展开';
}

function setEditorTitle(name) {
  const title = $('#editor-title');
  if (title) title.textContent = name ? `编辑：${name}` : '添加商品';
}
const PAGE_SIZE = 20;
const listState = {
  users: { page: 0 },
  orders: { page: 0 },
  afterSales: { page: 0 },
  reviews: { page: 0 },
};

function qs(params) {
  const search = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && String(value).trim() !== '') search.set(key, String(value).trim());
  });
  const text = search.toString();
  return text ? `?${text}` : '';
}

function renderPager(elementId, state, total, reload) {
  const pages = Math.max(1, Math.ceil(total / PAGE_SIZE));
  const node = $(elementId);
  node.innerHTML = `<span>共 ${total} 条</span>
    <button type="button" class="secondary" data-d="-1" ${state.page <= 0 ? 'disabled' : ''}>上一页</button>
    <span>${state.page + 1} / ${pages}</span>
    <button type="button" class="secondary" data-d="1" ${state.page + 1 >= pages ? 'disabled' : ''}>下一页</button>`;
  node.querySelectorAll('button').forEach((button) => {
    button.onclick = () => {
      if (button.disabled) return;
      state.page += Number(button.dataset.d);
      reload();
    };
  });
}

async function loadUsers() {
  const q = $('#users-q') ? $('#users-q').value : '';
  const data = await request(`/admin/users${qs({ q, page: listState.users.page, size: PAGE_SIZE })}`);
  const items = data.items || [];
  $('#users-table').innerHTML = `<table><thead><tr><th>ID</th><th>手机号</th><th>创建时间</th><th>更新时间</th></tr></thead><tbody>${items.map((item) => `<tr><td>${escapeHtml(item.id)}</td><td>${escapeHtml(item.phone)}</td><td>${new Date(item.createdAt).toLocaleString()}</td><td>${new Date(item.updatedAt).toLocaleString()}</td></tr>`).join('') || '<tr><td colspan="4">没有匹配的用户</td></tr>'}</tbody></table>`;
  renderPager('#users-pager', listState.users, data.total || 0, loadUsers);
}
async function loadReviews() {
  const q = $('#reviews-q') ? $('#reviews-q').value : '';
  const status = $('#reviews-status') ? $('#reviews-status').value : '';
  const data = await request(`/admin/reviews${qs({ q, status, page: listState.reviews.page, size: PAGE_SIZE })}`);
  reviews = data.items || [];
  $('#reviews-table').innerHTML = `<table><thead><tr><th>订单</th><th>用户</th><th>评分</th><th>内容</th><th>状态</th><th>时间</th><th>操作</th></tr></thead><tbody>${reviews.map((item) => `<tr><td>${escapeHtml(item.orderId)}</td><td>${escapeHtml(item.userId)}</td><td>${item.rating} 星</td><td>${escapeHtml(item.content)}</td><td>${escapeHtml(item.status)}</td><td>${new Date(item.createdAt).toLocaleString()}</td><td>${item.status === 'PENDING_REVIEW' ? `<button class="primary approve-review" data-id="${item.id}">通过</button> <button class="secondary reject-review" data-id="${item.id}">拒绝</button>` : ''}</td></tr>`).join('') || '<tr><td colspan="7">没有匹配的评价</td></tr>'}</tbody></table>`;
  document.querySelectorAll('.approve-review').forEach((button) => button.onclick = () => moderateReview(button.dataset.id, 'approve'));
  document.querySelectorAll('.reject-review').forEach((button) => button.onclick = () => moderateReview(button.dataset.id, 'reject'));
  renderPager('#reviews-pager', listState.reviews, data.total || 0, loadReviews);
}
async function moderateReview(id, action) { await request(`/admin/reviews/${id}/${action}`, { method: 'POST' }); loadReviews(); }
async function loadOrders() {
  const q = $('#orders-q') ? $('#orders-q').value : '';
  const status = $('#orders-status') ? $('#orders-status').value : '';
  const data = await request(`/admin/orders${qs({ q, status, page: listState.orders.page, size: PAGE_SIZE })}`);
  const items = data.items || [];
  $('#orders-table').innerHTML = `<table><thead><tr><th>订单</th><th>商品</th><th>金额</th><th>收货</th><th>状态</th><th>物流</th><th>创建时间</th><th>操作</th></tr></thead><tbody>${items.map((item) => `<tr><td>${escapeHtml(item.id)}</td><td>${escapeHtml(item.title)}</td><td>¥${Number(item.amount).toFixed(2)}</td><td>${escapeHtml(item.receiverLine || '未填地址')}</td><td>${escapeHtml(item.status)}</td><td>${shippingLabel(item)}</td><td>${new Date(item.createdAt).toLocaleString()}</td><td>${orderActions(item)}</td></tr>`).join('') || '<tr><td colspan="8">没有匹配的订单</td></tr>'}</tbody></table>`;
  document.querySelectorAll('.cancel-order').forEach((button) => button.onclick = async () => { await request(`/admin/orders/${button.dataset.id}/cancel`, { method: 'POST' }); loadOrders(); });
  document.querySelectorAll('.ship-express').forEach((button) => button.onclick = () => shipExpress(button.dataset.id));
  document.querySelectorAll('.ship-none').forEach((button) => button.onclick = () => shipNone(button.dataset.id));
  renderPager('#orders-pager', listState.orders, data.total || 0, loadOrders);
}

function shippingLabel(item) {
  if (item.shippedAt) {
    if (!item.trackingNo || item.trackingNo === 'NONE') return '无需物流已上报';
    return `${escapeHtml(item.expressCompany || '')} ${escapeHtml(item.trackingNo)}`;
  }
  return '未发货';
}

function orderActions(item) {
  const actions = [];
  if (item.status === 'PENDING_PAYMENT') actions.push(`<button class="secondary cancel-order" data-id="${item.id}">取消</button>`);
  if ((item.status === 'PAID' || item.status === 'PROCESSING') && !item.shippedAt) {
    actions.push(`<button class="primary ship-express" data-id="${item.id}">填单号</button>`);
    actions.push(`<button class="secondary ship-none" data-id="${item.id}">无需物流</button>`);
  }
  return actions.join(' ');
}

async function shipExpress(id) {
  const trackingNo = prompt('线下寄出后的物流单号（不对接快递接口，手填即可）');
  if (trackingNo === null || !trackingNo.trim()) return;
  const expressCompany = prompt('物流公司编码：SF / YTO / ZTO / YD / STO / JD / EMS', 'SF');
  if (expressCompany === null || !expressCompany.trim()) return;
  await request(`/admin/orders/${id}/ship`, { method: 'POST', body: JSON.stringify({ mode: 'EXPRESS', trackingNo: trackingNo.trim(), expressCompany: expressCompany.trim() }) });
  loadOrders();
}

async function shipNone(id) {
  if (!confirm('按「无需物流」上报微信。不需要开通快递服务，也不需要单号。确定？')) return;
  await request(`/admin/orders/${id}/ship`, { method: 'POST', body: JSON.stringify({ mode: 'NONE' }) });
  loadOrders();
}

async function loadAfterSales() {
  const data = await request(`/admin/after-sales${qs({ page: listState.afterSales.page, size: PAGE_SIZE })}`);
  const items = data.items || [];
  $('#after-sales-table').innerHTML = `<table><thead><tr><th>售后单</th><th>订单</th><th>用户</th><th>原因</th><th>状态</th><th>时间</th><th>操作</th></tr></thead><tbody>${items.map((item) => `<tr><td>${escapeHtml(item.id)}</td><td>${escapeHtml(item.orderId)}</td><td>${escapeHtml(item.userId)}</td><td>${escapeHtml(item.reason)}</td><td>${escapeHtml(item.status)}</td><td>${new Date(item.createdAt).toLocaleString()}</td><td>${item.status === 'REQUESTED' ? `<button class="primary approve-after-sale" data-id="${item.id}">同意退款</button> <button class="secondary reject-after-sale" data-id="${item.id}">拒绝</button>` : ''}</td></tr>`).join('') || '<tr><td colspan="7">没有售后单</td></tr>'}</tbody></table>`;
  document.querySelectorAll('.approve-after-sale').forEach((button) => button.onclick = async () => { await request(`/admin/after-sales/${button.dataset.id}/approve`, { method: 'POST' }); loadAfterSales(); });
  document.querySelectorAll('.reject-after-sale').forEach((button) => button.onclick = async () => { await request(`/admin/after-sales/${button.dataset.id}/reject`, { method: 'POST' }); loadAfterSales(); });
  renderPager('#after-sales-pager', listState.afterSales, data.total || 0, loadAfterSales);
}
function editProduct(id) {
  const product = products.find((item) => item.id === id);
  const form = $('#product-form');
  form.reset();
  Object.entries(product).forEach(([key, value]) => {
    if (form.elements[key]) form.elements[key].type === 'checkbox' ? form.elements[key].checked = value : form.elements[key].value = value ?? '';
  });
  draftTypes = (product.types && product.types.length ? product.types : []).map((item) => ({
    ...emptyType(),
    ...item,
    file: null,
    preview: productImageSrc(item),
  }));
  renderTypes();
  showImagePreview(productImageSrc(product));
  setEditorTitle(product.name);
  $('#product-form-wrap').classList.remove('hidden');
  setEditorOpen(true);
  $('#product-form-wrap').scrollIntoView({ behavior: 'smooth', block: 'start' });
  const error = $('#page-error');
  const typeError = $('#type-error');
  if (error) error.textContent = '';
  if (typeError) typeError.textContent = '';
}
async function deleteProduct(id) { if (!confirm('确定下架该商品？历史订单仍会保留。')) return; await request(`/admin/products/${id}`, { method: 'DELETE' }); loadProducts(); }

function couponLabel(item) {
  if (!item) return '未选择';
  const threshold = Number(item.thresholdAmount || 0);
  const discount = Number(item.discountAmount || 0).toFixed(2);
  return `${item.name} · ${threshold > 0 ? `满${threshold}减${discount}` : `无门槛减${discount}`}${item.active ? '' : '（停用）'}`;
}

let couponProducts = [];

function renderCouponScope(selectedIds) {
  const box = $('#coupon-product-scope');
  if (!box) return;
  const selected = new Set(selectedIds || []);
  const zones = new Map();
  couponProducts.forEach((product) => {
    const zone = product.zone || '未分区';
    if (!zones.has(zone)) zones.set(zone, []);
    zones.get(zone).push(product);
  });
  box.innerHTML = [...zones.entries()].map(([zone, items]) => {
    const all = items.length > 0 && items.every((product) => selected.has(product.id));
    return `<div class="scope-zone">
      <label class="checkbox"><input type="checkbox" class="zone-all" data-zone="${escapeHtml(zone)}" ${all ? 'checked' : ''}> ${escapeHtml(zone)} 全选</label>
      <div class="scope-products">${items.map((product) => `<label class="checkbox"><input type="checkbox" name="productId" value="${escapeHtml(product.id)}" data-zone="${escapeHtml(zone)}" ${selected.has(product.id) ? 'checked' : ''}> ${escapeHtml(product.name)}${product.active ? '' : '（已下架）'}</label>`).join('')}</div>
    </div>`;
  }).join('') || '<p class="muted">还没有商品</p>';
  box.querySelectorAll('.zone-all').forEach((boxInput) => {
    boxInput.onchange = () => {
      const zone = boxInput.dataset.zone;
      box.querySelectorAll('input[name="productId"]').forEach((input) => {
        if (input.dataset.zone === zone) input.checked = boxInput.checked;
      });
    };
  });
  box.querySelectorAll('input[name="productId"]').forEach((input) => {
    input.onchange = () => {
      const zone = input.dataset.zone;
      const peers = [...box.querySelectorAll('input[name="productId"]')].filter((item) => item.dataset.zone === zone);
      const all = [...box.querySelectorAll('.zone-all')].find((item) => item.dataset.zone === zone);
      if (all) all.checked = peers.every((item) => item.checked);
    };
  });
}

function selectedCouponProducts() {
  return [...document.querySelectorAll('#coupon-product-scope input[name="productId"]:checked')].map((input) => input.value);
}

function fillTemplateSelect(select, templates, selectedId) {
  if (!select) return;
  select.innerHTML = `<option value="">不发放</option>${templates.map((item) => `<option value="${escapeHtml(item.id)}">${escapeHtml(couponLabel(item))}</option>`).join('')}`;
  select.value = selectedId || '';
}

async function loadCoupons() {
  const [templates, policy, products] = await Promise.all([
    request('/admin/coupons/templates'),
    request('/admin/coupons/policy'),
    request('/admin/products'),
  ]);
  couponProducts = products || [];
  const form = $('#coupon-policy-form');
  form.elements.enabled.checked = !!policy.enabled;
  fillTemplateSelect(form.elements.inviteTemplateId, templates, policy.inviteTemplateId);
  fillTemplateSelect(form.elements.purchaseTemplateId, templates, policy.purchaseTemplateId);
  form.elements.purchaseThreshold.value = policy.purchaseThreshold ?? 0;
  $('#coupon-templates-table').innerHTML = `<table><thead><tr><th>名称</th><th>规则</th><th>可用商品</th><th>每单上限</th><th>状态</th><th>操作</th></tr></thead><tbody>${(templates || []).map((item) => `<tr>
    <td>${escapeHtml(item.name)}</td>
    <td>${Number(item.thresholdAmount) > 0 ? `满 ${Number(item.thresholdAmount).toFixed(2)} 减 ${Number(item.discountAmount).toFixed(2)}` : `无门槛减 ${Number(item.discountAmount).toFixed(2)}`}</td>
    <td>${escapeHtml(item.scopeText || '未指定')}</td>
    <td>${item.perOrderLimit || 1} 张</td>
    <td>${item.active ? '启用' : '停用'}</td>
    <td><button class="secondary edit-coupon" data-id="${item.id}">编辑</button></td>
  </tr>`).join('') || '<tr><td colspan="6">还没有模板</td></tr>'}</tbody></table>`;
  document.querySelectorAll('.edit-coupon').forEach((button) => {
    button.onclick = () => {
      const item = templates.find((row) => row.id === button.dataset.id);
      if (!item) return;
      const templateForm = $('#coupon-template-form');
      templateForm.classList.remove('hidden');
      templateForm.elements.id.value = item.id;
      templateForm.elements.name.value = item.name;
      templateForm.elements.thresholdAmount.value = item.thresholdAmount;
      templateForm.elements.discountAmount.value = item.discountAmount;
      templateForm.elements.perOrderLimit.value = item.perOrderLimit || 1;
      templateForm.elements.active.checked = !!item.active;
      renderCouponScope(item.productIds || []);
    };
  });
}

async function loadAnnouncements() {
  const items = await request('/admin/announcements');
  $('#announcements-table').innerHTML = `<table><thead><tr><th>标题</th><th>状态</th><th>更新时间</th><th>操作</th></tr></thead><tbody>${(items || []).map((item) => `<tr>
    <td>${escapeHtml(item.title)}<br><small>${escapeHtml((item.content || '').slice(0, 48))}</small></td>
    <td>${item.published ? '已发布' : '草稿'}</td>
    <td>${new Date(item.updatedAt).toLocaleString()}</td>
    <td><button class="secondary edit-announcement" data-id="${item.id}">编辑</button> <button class="secondary delete-announcement" data-id="${item.id}">删除</button></td>
  </tr>`).join('') || '<tr><td colspan="4">还没有公告</td></tr>'}</tbody></table>`;
  document.querySelectorAll('.edit-announcement').forEach((button) => {
    button.onclick = () => {
      const item = items.find((row) => row.id === button.dataset.id);
      if (!item) return;
      const form = $('#announcement-form');
      form.classList.remove('hidden');
      form.elements.id.value = item.id;
      form.elements.title.value = item.title;
      form.elements.content.value = item.content;
      form.elements.published.checked = !!item.published;
    };
  });
  document.querySelectorAll('.delete-announcement').forEach((button) => {
    button.onclick = async () => {
      if (!confirm('确定删除该公告？')) return;
      await request(`/admin/announcements/${button.dataset.id}`, { method: 'DELETE' });
      loadAnnouncements();
    };
  });
}

$('#login-form').onsubmit = async (event) => { event.preventDefault(); try { const data = await request('/admin/auth/login', { method: 'POST', body: JSON.stringify(Object.fromEntries(new FormData(event.target))) }); token = data.accessToken; sessionStorage.setItem('haode_admin_token', token); $('#login-view').classList.add('hidden'); $('#app-view').classList.remove('hidden'); showSection('overview'); } catch (error) { $('#login-error').textContent = error.message; } };
document.querySelectorAll('nav button').forEach((button) => button.onclick = () => showSection(button.dataset.section));
$('#users-search').onclick = () => { listState.users.page = 0; loadUsers(); };
$('#orders-search').onclick = () => { listState.orders.page = 0; loadOrders(); };
$('#reviews-search').onclick = () => { listState.reviews.page = 0; loadReviews(); };
['users-q', 'orders-q', 'reviews-q'].forEach((id) => {
  const input = document.getElementById(id);
  if (input) input.addEventListener('keydown', (event) => {
    if (event.key === 'Enter') document.getElementById(id.replace('-q', '-search')).click();
  });
});
$('#refresh').onclick = () => showSection(document.querySelector('.section.active-section').id);
$('#logout').onclick = () => { sessionStorage.removeItem('haode_admin_token'); location.reload(); };
$('#new-product').onclick = () => {
  $('#product-form').reset();
  $('#product-form').elements.id.value = '';
  showImagePreview('');
  draftTypes = [];
  renderTypes();
  setEditorTitle('');
  $('#product-form-wrap').classList.remove('hidden');
  setEditorOpen(true);
  const error = $('#page-error');
  const typeError = $('#type-error');
  if (error) error.textContent = '';
  if (typeError) typeError.textContent = '';
};
$('#cancel-product').onclick = () => {
  $('#product-form-wrap').classList.add('hidden');
  setEditorOpen(true);
};
const toggleEditor = $('#toggle-editor');
if (toggleEditor) {
  toggleEditor.onclick = () => {
    const wrap = $('#product-form-wrap');
    setEditorOpen(wrap.classList.contains('collapsed'));
  };
}
['products-q', 'products-zone', 'products-status'].forEach((id) => {
  const node = document.getElementById(id);
  if (!node) return;
  node.addEventListener(node.tagName === 'INPUT' ? 'input' : 'change', renderProductTable);
});
const addTypeButton = $('#add-product-type');
if (addTypeButton) {
  addTypeButton.onclick = () => {
    if (!currentProductId()) {
      const error = $('#type-error');
      if (error) error.textContent = '请先保存商品，再添加类型';
      return;
    }
    draftTypes.push(emptyType());
    renderTypes();
  };
}
const imageFileInput = $('#product-form').elements.imageFile;
if (imageFileInput) {
  imageFileInput.onchange = (event) => {
    const file = event.target.files && event.target.files[0];
    showImagePreview(file ? URL.createObjectURL(file) : ($('#product-form').elements.imageUrl.value || ''));
  };
}
$('#product-form').addEventListener('submit', (event) => {
  event.preventDefault();
  event.stopPropagation();
  return false;
});
const saveProductButton = $('#save-product');
if (saveProductButton) saveProductButton.onclick = () => saveProduct($('#product-form'));

async function saveProduct(form) {
  const error = $('#page-error');
  if (error) error.textContent = '';
  try {
    const file = form.elements.imageFile && form.elements.imageFile.files[0];
    const data = {
      name: form.elements.name.value.trim(),
      subtitle: form.elements.subtitle.value.trim(),
      price: Number(form.elements.price.value),
      originalPrice: form.elements.originalPrice.value ? Number(form.elements.originalPrice.value) : null,
      color: form.elements.color.value || null,
      accent: form.elements.accent.value || null,
      pattern: form.elements.pattern.value || null,
      tag: form.elements.tag.value || null,
      zone: form.elements.zone.value.trim(),
      homepage: form.elements.homepage.checked,
      sortOrder: Number(form.elements.sortOrder.value || 0),
      imageUrl: form.elements.imageUrl.value || null,
      active: form.elements.active.checked,
    };
    const id = (form.elements.id.value || '').trim();
    const saved = await request(id ? `/admin/products/${id}` : '/admin/products', {
      method: id ? 'PUT' : 'POST',
      body: JSON.stringify(data),
    });
    form.elements.id.value = saved.id;
    if (file) {
      const body = new FormData();
      body.append('file', file);
      await request(`/admin/products/${saved.id}/image`, { method: 'POST', body });
    }
    const existing = products.find((item) => item.id === saved.id);
    if (existing) Object.assign(existing, saved);
    else products.push(saved);
    setEditorTitle(saved.name);
    renderTypes();
    if (error) error.textContent = '商品已保存，可在下方提交类型，或点收起看列表';
    loadProducts();
  } catch (err) {
    if (error) error.textContent = err.message || '保存失败';
  }
}

const supportState = { userId: '', phone: '', messages: [], poll: null };

function supportAvatar(thread) {
  if (thread.avatarUrl) return `<img class="wx-avatar" src="${escapeHtml(productImageSrc({ imageUrl: thread.avatarUrl }))}" alt="">`;
  const mark = (thread.phone || thread.userId || '客').slice(-2);
  return `<span class="wx-avatar">${escapeHtml(mark)}</span>`;
}

function formatChatTime(value) {
  if (!value) return '';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '';
  return `${date.getMonth() + 1}/${date.getDate()} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`;
}

function renderSupportThreads(threads) {
  const wrap = $('#support-threads');
  if (!wrap) return;
  wrap.innerHTML = threads.map((item) => `
    <div class="wx-thread ${item.userId === supportState.userId ? 'is-on' : ''}" data-id="${escapeHtml(item.userId)}" data-phone="${escapeHtml(item.phone || '')}">
      ${supportAvatar(item)}
      <div class="wx-meta">
        <div class="wx-name"><span>${escapeHtml(item.phone || item.userId)}</span><span class="wx-time">${escapeHtml(formatChatTime(item.lastAt))}</span></div>
        <span class="wx-preview">${item.unread ? `<b class="wx-unread">${item.unread > 99 ? '99+' : item.unread}</b> ` : ''}${escapeHtml(item.lastContent || '')}</span>
      </div>
    </div>
  `).join('') || '<p class="muted" style="padding:16px">还没有用户发起会话</p>';
  wrap.querySelectorAll('.wx-thread').forEach((node) => {
    node.onclick = () => openSupportThread(node.dataset.id, node.dataset.phone);
  });
}

function renderSupportLog() {
  const log = $('#support-log');
  if (!log) return;
  if (!supportState.userId) {
    log.innerHTML = '<p class="wx-empty">从左侧选择用户开始回复</p>';
    return;
  }
  log.innerHTML = supportState.messages.map((item) => `
    <div class="wx-row ${item.sender === 'ADMIN' ? 'is-admin' : ''}"><div class="wx-bubble">${escapeHtml(item.content)}</div></div>
  `).join('') || '<p class="wx-empty">还没有消息，等待用户咨询</p>';
  log.scrollTop = log.scrollHeight;
}

async function loadSupportSettings() {
  const data = await request('/admin/support/settings');
  if ($('#support-wechat')) $('#support-wechat').value = data.wechatId || '';
}

async function loadSupportThreads() {
  const q = ($('#support-q') && $('#support-q').value) || '';
  const threads = await request(`/admin/support/threads${qs({ q })}`);
  renderSupportThreads(Array.isArray(threads) ? threads : []);
}

async function openSupportThread(userId, phone) {
  supportState.userId = userId;
  supportState.phone = phone || '';
  supportState.messages = [];
  $('#support-peer').textContent = phone ? `与 ${phone} 的会话` : userId;
  await loadSupportMessages(true);
  loadSupportThreads();
}

async function loadSupportMessages(reset) {
  if (!supportState.userId) return;
  const after = !reset && supportState.messages.length
    ? supportState.messages[supportState.messages.length - 1].createdAt
    : '';
  const incoming = await request(`/admin/support/threads/${encodeURIComponent(supportState.userId)}/messages${qs({ after })}`);
  const list = Array.isArray(incoming) ? incoming : [];
  if (reset) supportState.messages = list;
  else {
    const seen = new Set(supportState.messages.map((item) => item.id));
    list.forEach((item) => { if (!seen.has(item.id)) supportState.messages.push(item); });
  }
  renderSupportLog();
}

async function sendSupportMessage() {
  const input = $('#support-input');
  const content = (input.value || '').trim();
  if (!supportState.userId || !content) return;
  const saved = await request(`/admin/support/threads/${encodeURIComponent(supportState.userId)}/messages`, {
    method: 'POST',
    body: JSON.stringify({ content }),
  });
  supportState.messages.push(saved);
  input.value = '';
  renderSupportLog();
  loadSupportThreads();
}

function startSupport() {
  loadSupportSettings();
  loadSupportThreads();
  if (supportState.userId) loadSupportMessages(true);
  else renderSupportLog();
  stopSupport();
  supportState.poll = setInterval(() => {
    loadSupportThreads().catch(() => {});
    if (supportState.userId) loadSupportMessages(false).catch(() => {});
  }, 2500);
}

function stopSupport() {
  if (supportState.poll) clearInterval(supportState.poll);
  supportState.poll = null;
}

if ($('#save-wechat')) {
  $('#save-wechat').onclick = async () => {
    await request('/admin/support/settings', {
      method: 'PUT',
      body: JSON.stringify({ wechatId: $('#support-wechat').value.trim() }),
    });
    const error = $('#page-error');
    if (error) error.textContent = '客服微信号已保存';
  };
}
if ($('#support-send-btn')) $('#support-send-btn').onclick = () => sendSupportMessage().catch((err) => { const error = $('#page-error'); if (error) error.textContent = err.message; });
if ($('#support-input')) {
  $('#support-input').addEventListener('keydown', (event) => {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      sendSupportMessage().catch((err) => { const error = $('#page-error'); if (error) error.textContent = err.message; });
    }
  });
}
if ($('#support-q')) $('#support-q').addEventListener('input', () => loadSupportThreads().catch(() => {}));

if ($('#coupon-policy-form')) {
  $('#coupon-policy-form').onsubmit = async (event) => {
    event.preventDefault();
    const form = event.target;
    await request('/admin/coupons/policy', {
      method: 'PUT',
      body: JSON.stringify({
        enabled: form.elements.enabled.checked,
        inviteTemplateId: form.elements.inviteTemplateId.value || null,
        purchaseTemplateId: form.elements.purchaseTemplateId.value || null,
        purchaseThreshold: Number(form.elements.purchaseThreshold.value || 0),
      }),
    });
    const error = $('#page-error');
    if (error) error.textContent = '邀请政策已保存';
    loadCoupons();
  };
}
if ($('#new-coupon-template')) {
  $('#new-coupon-template').onclick = () => {
    const form = $('#coupon-template-form');
    form.reset();
    form.elements.id.value = '';
    form.elements.active.checked = true;
    form.elements.perOrderLimit.value = 1;
    form.classList.remove('hidden');
    renderCouponScope([]);
  };
}
if ($('#cancel-coupon-template')) {
  $('#cancel-coupon-template').onclick = () => $('#coupon-template-form').classList.add('hidden');
}
if ($('#coupon-template-form')) {
  $('#coupon-template-form').onsubmit = async (event) => {
    event.preventDefault();
    const form = event.target;
    const id = (form.elements.id.value || '').trim();
    const productIds = selectedCouponProducts();
    if (productIds.length === 0) {
      const error = $('#page-error');
      if (error) error.textContent = '请至少勾选一个可用商品';
      return;
    }
    await request(id ? `/admin/coupons/templates/${id}` : '/admin/coupons/templates', {
      method: id ? 'PUT' : 'POST',
      body: JSON.stringify({
        name: form.elements.name.value.trim(),
        thresholdAmount: Number(form.elements.thresholdAmount.value),
        discountAmount: Number(form.elements.discountAmount.value),
        perOrderLimit: Number(form.elements.perOrderLimit.value || 1),
        active: form.elements.active.checked,
        productIds,
      }),
    });
    form.classList.add('hidden');
    loadCoupons();
  };
}
if ($('#new-announcement')) {
  $('#new-announcement').onclick = () => {
    const form = $('#announcement-form');
    form.reset();
    form.elements.id.value = '';
    form.elements.published.checked = true;
    form.classList.remove('hidden');
  };
}
if ($('#cancel-announcement')) {
  $('#cancel-announcement').onclick = () => $('#announcement-form').classList.add('hidden');
}
if ($('#announcement-form')) {
  $('#announcement-form').onsubmit = async (event) => {
    event.preventDefault();
    const form = event.target;
    const id = (form.elements.id.value || '').trim();
    await request(id ? `/admin/announcements/${id}` : '/admin/announcements', {
      method: id ? 'PUT' : 'POST',
      body: JSON.stringify({
        title: form.elements.title.value.trim(),
        content: form.elements.content.value.trim(),
        published: form.elements.published.checked,
      }),
    });
    form.classList.add('hidden');
    loadAnnouncements();
  };
}

if (token) { $('#login-view').classList.add('hidden'); $('#app-view').classList.remove('hidden'); showSection('overview'); }
