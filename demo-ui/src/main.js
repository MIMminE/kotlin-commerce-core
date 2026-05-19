const productList = document.querySelector("#productList");
const productSelect = document.querySelector("select[name='productId']");
const orderList = document.querySelector("#orderList");
const logOutput = document.querySelector("#logOutput");
const serviceStatus = document.querySelector("#serviceStatus");
const statusText = document.querySelector("#statusText");
const cartSummary = document.querySelector("#cartSummary");
const qtyInput = document.querySelector("input[name='qty']");
const searchInput = document.querySelector("#searchInput");
const categoryButtons = document.querySelectorAll(".category-chip");

let products = [];
let currentUserId = "user-123";
let activeCategory = "전체";
const categories = ["데일리 추천", "프리미엄", "베스트", "스마트홈", "액세서리"];
const visualTypes = ["box", "device", "bottle", "component", "accessory"];

const currency = new Intl.NumberFormat("ko-KR", {
  style: "currency",
  currency: "KRW",
  maximumFractionDigits: 0
});

function log(message, data) {
  const time = new Date().toLocaleTimeString("ko-KR", { hour12: false });
  const detail = data ? `\n${JSON.stringify(data, null, 2)}` : "";
  logOutput.textContent = `[${time}] ${message}${detail}\n\n${logOutput.textContent}`;
}

async function request(url, options = {}) {
  const response = await fetch(url, {
    headers: {
      "Content-Type": "application/json",
      "X-API-Key": "dev-api-key-5678",
      ...(options.headers ?? {})
    },
    ...options
  });

  if (!response.ok) {
    const body = await response.text();
    throw new Error(`${response.status} ${response.statusText}: ${body}`);
  }

  if (response.status === 204) return null;
  return response.json();
}

function setServiceStatus(ok, text) {
  serviceStatus.classList.toggle("is-ok", ok);
  serviceStatus.classList.toggle("is-fail", !ok);
  statusText.textContent = text;
}

function productPrice(product) {
  if (typeof product.price === "number") return product.price;
  return product.price?.amount ?? 0;
}

function productCurrency(product) {
  if (typeof product.price === "object") return product.price?.currency ?? "KRW";
  return "KRW";
}

function selectedProduct() {
  return products.find((product) => product.productId === productSelect.value) ?? products[0];
}

function productCategory(product) {
  const index = products.findIndex((item) => item.productId === product.productId);
  return categories[Math.max(index, 0) % categories.length];
}

function productVisual(product) {
  const index = products.findIndex((item) => item.productId === product.productId);
  return visualTypes[Math.max(index, 0) % visualTypes.length];
}

function visibleProducts() {
  const query = searchInput.value.trim().toLowerCase();
  return products.filter((product) => {
    const category = productCategory(product);
    const matchesCategory = activeCategory === "전체" || category === activeCategory;
    const matchesQuery = product.productName.toLowerCase().includes(query);
    return matchesCategory && matchesQuery;
  });
}

function updateCartSummary() {
  const product = selectedProduct();
  const qty = Number(qtyInput.value || 1);

  if (!product) {
    cartSummary.innerHTML = `<div class="empty">상품을 먼저 선택해 주세요.</div>`;
    return;
  }

  const price = productPrice(product);
  cartSummary.innerHTML = `
    <div class="summary-line">
      <span>${product.productName}</span>
      <strong>${currency.format(price)}</strong>
    </div>
    <div class="summary-line">
      <span>수량</span>
      <strong>${qty}</strong>
    </div>
    <div class="summary-line total">
      <span>결제 예정 금액</span>
      <strong>${currency.format(price * qty)}</strong>
    </div>
  `;
}

function renderProducts() {
  productList.innerHTML = "";
  productSelect.innerHTML = "";

  const shownProducts = visibleProducts();

  if (shownProducts.length === 0) {
    productList.innerHTML = `<div class="empty">조건에 맞는 상품이 없습니다.</div>`;
    updateCartSummary();
    return;
  }

  shownProducts.forEach((product) => {
    const price = productPrice(product);
    const category = productCategory(product);
    const visual = productVisual(product);
    const option = document.createElement("option");
    option.value = product.productId;
    option.textContent = `${product.productName} · ${currency.format(price)}`;
    option.dataset.price = String(price);
    option.dataset.currency = productCurrency(product);
    productSelect.append(option);

    const card = document.createElement("article");
    card.className = "product-card";
    card.innerHTML = `
      <div class="product-media product-media-${visual}">
        <span class="product-shot" aria-hidden="true">
          <span></span>
          <span></span>
        </span>
        <span class="stock-chip">재고 ${product.stock ?? "-"}</span>
      </div>
      <div class="product-title">
        <strong>${product.productName}</strong>
        <small>${category}</small>
      </div>
      <div class="product-footer">
        <span class="price">${currency.format(price)}</span>
        <button class="add-button" type="button">담기</button>
      </div>
    `;

    card.querySelector(".add-button").addEventListener("click", () => {
      productSelect.value = product.productId;
      qtyInput.value = "1";
      updateCartSummary();
      document.querySelector("#checkout").scrollIntoView({ behavior: "smooth", block: "start" });
      log("상품 담기", { productId: product.productId, productName: product.productName });
    });

    productList.append(card);
  });

  updateCartSummary();
}

function renderOrders(page) {
  const orders = page?.content ?? [];
  orderList.innerHTML = "";

  if (orders.length === 0) {
    orderList.innerHTML = `<div class="empty">아직 주문 내역이 없습니다.</div>`;
    return;
  }

  for (const order of orders) {
    const row = document.createElement("div");
    row.className = "order-row";
    row.innerHTML = `
      <div class="order-row-top">
        <span class="order-status">${order.status}</span>
        <strong>${currency.format(order.totalAmount)}</strong>
      </div>
      <span class="order-id">주문번호 ${order.orderId.slice(0, 8)}</span>
    `;
    orderList.append(row);
  }
}

async function loadProducts() {
  const data = await request("/product-api/api/products/search/all");
  products = data.products ?? [];
  renderProducts();
  setServiceStatus(true, "온라인 주문 가능");
  log("상품 조회 완료", { size: products.length });
}

async function loadOrders(userId = currentUserId) {
  currentUserId = userId;
  const data = await request(`/order-api/api/orders?userId=${encodeURIComponent(userId)}&page=0&size=10`);
  renderOrders(data);
  log("주문 목록 조회 완료", { userId, totalElements: data.totalElements ?? 0 });
}

document.querySelector("#refreshProducts").addEventListener("click", async () => {
  try {
    await loadProducts();
  } catch (error) {
    setServiceStatus(false, "일시적으로 주문 불가");
    log("상품 조회 실패", { error: error.message });
  }
});

document.querySelector("#refreshOrders").addEventListener("click", async () => {
  try {
    const userId = document.querySelector("input[name='userId']").value.trim();
    await loadOrders(userId);
  } catch (error) {
    log("주문 조회 실패", { error: error.message });
  }
});

document.querySelector("#productForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  const form = new FormData(event.currentTarget);

  try {
    const data = await request("/product-api/api/products", {
      method: "POST",
      headers: { "Idempotency-Key": crypto.randomUUID() },
      body: JSON.stringify({
        productName: form.get("productName"),
        price: Number(form.get("price")),
        currency: "KRW"
      })
    });
    event.currentTarget.reset();
    log("상품 등록 완료", data);
    await loadProducts();
  } catch (error) {
    log("상품 등록 실패", { error: error.message });
  }
});

document.querySelector("#orderForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  const form = new FormData(event.currentTarget);
  const selected = productSelect.selectedOptions[0];
  const qty = Number(form.get("qty"));
  const unitPriceAmount = Number(selected?.dataset.price ?? 0);
  const unitPriceCurrency = selected?.dataset.currency ?? "KRW";
  const userId = String(form.get("userId")).trim();

  try {
    const data = await request("/order-api/api/orders", {
      method: "POST",
      body: JSON.stringify({
        idempotencyKey: crypto.randomUUID(),
        userId,
        items: [
          {
            productId: form.get("productId"),
            qty,
            unitPriceAmount,
            unitPriceCurrency
          }
        ],
        totalAmount: unitPriceAmount * qty,
        currency: unitPriceCurrency
      })
    });
    log("주문 생성 완료", data);
    await loadOrders(userId);
  } catch (error) {
    log("주문 생성 실패", { error: error.message });
  }
});

document.querySelector("#clearLog").addEventListener("click", () => {
  logOutput.textContent = "";
});

productSelect.addEventListener("change", updateCartSummary);
qtyInput.addEventListener("input", updateCartSummary);
searchInput.addEventListener("input", renderProducts);
categoryButtons.forEach((button) => {
  button.addEventListener("click", () => {
    activeCategory = button.dataset.category;
    categoryButtons.forEach((item) => item.classList.toggle("is-active", item === button));
    renderProducts();
  });
});

loadProducts()
  .then(() => loadOrders(currentUserId))
  .catch((error) => {
    setServiceStatus(false, "일시적으로 주문 불가");
    log("초기 데이터 로딩 실패", { error: error.message });
  });
