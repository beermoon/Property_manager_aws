(() => {
    const focusId = window.__FOCUS_ID__ ?? null;

    const map = new naver.maps.Map('map', {
        center: new naver.maps.LatLng(37.5665, 126.9780),
        zoom: 12
    });

    let markers = [];
    let infoWindows = [];
    const markerMap = new Map();

    function setHint(text) {
        const el = document.getElementById('hint');
        if (el) el.textContent = text || '';
    }

    function clearMarkers() {
        markers.forEach(m => m.setMap(null));
        markers = [];
        infoWindows.forEach(iw => iw.close());
        infoWindows = [];
        markerMap.clear();
    }

    function closeAllInfo() {
        infoWindows.forEach(iw => iw.close());
    }

    naver.maps.Event.addListener(map, 'click', closeAllInfo);
    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') closeAllInfo();
    });

    function readValue(id) {
        const el = document.getElementById(id);
        return el ? el.value.trim() : '';
    }

    function addParam(params, key, raw) {
        if (raw === null || raw === undefined) return;
        const v = String(raw).trim();
        if (v === '') return;
        params.append(key, v);
    }

    function buildQuery() {
        const params = new URLSearchParams();
        addParam(params, 'keyword', readValue('keyword'));
        addParam(params, 'region', readValue('region'));
        addParam(params, 'dealType', readValue('dealType'));
        addParam(params, 'minDeposit', readValue('minDeposit'));
        addParam(params, 'maxDeposit', readValue('maxDeposit'));
        addParam(params, 'minMonthlyRent', readValue('minMonthlyRent'));
        addParam(params, 'maxMonthlyRent', readValue('maxMonthlyRent'));
        addParam(params, 'minBuiltYear', readValue('minBuiltYear'));
        addParam(params, 'maxBuiltYear', readValue('maxBuiltYear'));
        addParam(params, 'roomCount', readValue('roomCount'));
        addParam(params, 'hasElevator', readValue('hasElevator'));
        addParam(params, 'hasParking', readValue('hasParking'));
        addParam(params, 'petAllowed', readValue('petAllowed'));
        addParam(params, 'lhAvailable', readValue('lhAvailable'));
        addParam(params, 'minArea', readValue('minArea'));
        addParam(params, 'maxArea', readValue('maxArea'));
        return params.toString();
    }

    function hasAnyFilter() {
        const ids = [
            'keyword',
            'region','dealType',
            'minDeposit','maxDeposit','minMonthlyRent','maxMonthlyRent',
            'minBuiltYear','maxBuiltYear',
            'roomCount','hasElevator','hasParking','petAllowed','lhAvailable',
            'minArea','maxArea'
        ];
        return ids.some(id => readValue(id) !== '');
    }

    function buildInfoHtml(p) {
        const dealLine =
            (p.dealType === 'MONTHLY')
                ? ` · 보/월 ${p.depositMan ?? '-'} / ${p.monthlyRentMan ?? '-'}만`
                : (p.dealType === 'JEONSE')
                    ? ` · 전세 ${p.depositMan ?? '-'}만`
                    : '';

        const tenant = (p.tenantPhone && String(p.tenantPhone).trim() !== '') ? p.tenantPhone : '-';
        const owner  = (p.ownerPhone && String(p.ownerPhone).trim() !== '') ? p.ownerPhone : '-';

        return `
    <div style="position:relative; padding:10px 34px 10px 10px; min-width:260px;">
      <button type="button" class="iw-close" aria-label="닫기"></button>

      <div style="font-weight:700; margin-bottom:6px;">${p.title ?? ''}</div>

      <div style="margin:6px 0; color:#666; font-size:12px;">
        ${p.address ?? ''}
      </div>

      <div style="margin:6px 0; font-size:12px; color:#444;">
        ${(p.dealTypeLabel ?? p.dealType ?? '')} · ${(p.statusLabel ?? p.status ?? '')}
        ${dealLine}
      </div>

      <div style="margin-top:8px; padding-top:8px; border-top:1px solid #eee; font-size:12px; color:#374151;">
        <div>세입자: <span style="color:#111827;">${tenant}</span></div>
        <div>건물주: <span style="color:#111827;">${owner}</span></div>
      </div>

      <div style="margin-top:8px;">
        <a href="/properties/${p.id}">상세보기</a>
      </div>
    </div>
  `;
    }

    function wireCloseButton(info) {
        // InfoWindow DOM 생성 이후에만 접근 가능
        setTimeout(() => {
            const root = info.getElement?.();
            if (!root) return;
            const btn = root.querySelector('.iw-close');
            if (btn) btn.onclick = () => info.close();
        }, 0);
    }

    function addOneMarker(p) {
        if (p.lat == null || p.lng == null) return;

        const pid = Number(p.id);
        const latlng = new naver.maps.LatLng(p.lat, p.lng);
        const marker = new naver.maps.Marker({ position: latlng, map });

        const info = new naver.maps.InfoWindow({ content: buildInfoHtml(p) });

        naver.maps.Event.addListener(marker, 'click', () => {
            if (info.getMap()) { info.close(); return; }
            closeAllInfo();
            info.open(map, marker);
            wireCloseButton(info);
        });

        markers.push(marker);
        infoWindows.push(info);
        markerMap.set(pid, { marker, info, latlng });
    }

    async function loadMarkers() {
        // A) 상세 -> 지도에서 보기
        if (focusId != null) {
            clearMarkers();
            setHint('선택한 매물을 불러오는 중...');

            try {
                const url = `/api/properties?id=${encodeURIComponent(focusId)}`;
                const res = await fetch(url);
                const list = await res.json();

                clearMarkers();
                setHint(`표시된 매물: ${Array.isArray(list) ? list.length : 0}건`);
                if (!Array.isArray(list) || list.length === 0) return;

                const p0 = list[0];
                if (p0.lat != null && p0.lng != null) {
                    map.setCenter(new naver.maps.LatLng(p0.lat, p0.lng));
                    map.setZoom(16);
                }

                list.forEach(addOneMarker);

                const target = markerMap.get(Number(p0.id));
                if (target) {
                    closeAllInfo();
                    target.info.open(map, target.marker);
                    wireCloseButton(target.info);
                }
            } catch (e) {
                console.error(e);
                setHint('선택한 매물 로딩 실패');
            }
            return;
        }

        // B) 필터 검색
        if (!hasAnyFilter()) {
            clearMarkers();
            setHint('필터를 설정하고 [적용]을 누르면 매물이 표시됩니다.');
            return;
        }

        try {
            const qs = buildQuery();
            const res = await fetch(`/api/properties?${qs}`);
            const list = await res.json();

            clearMarkers();
            setHint(`표시된 매물: ${Array.isArray(list) ? list.length : 0}건`);
            if (!Array.isArray(list) || list.length === 0) return;

            list.forEach(addOneMarker);

            const p0 = list[0];
            if (p0.lat != null && p0.lng != null) {
                map.setCenter(new naver.maps.LatLng(p0.lat, p0.lng));
            }
        } catch (e) {
            console.error(e);
            setHint('마커 로딩 실패');
        }
    }

    function resetFilters() {
        const ids = [
            'keyword','region','dealType',
            'minDeposit','maxDeposit','minMonthlyRent','maxMonthlyRent',
            'minBuiltYear','maxBuiltYear',
            'roomCount','hasElevator','hasParking','petAllowed','lhAvailable',
            'minArea','maxArea'
        ];
        ids.forEach(id => {
            const el = document.getElementById(id);
            if (el) el.value = '';
        });
    }

    document.getElementById('btnSearch')?.addEventListener('click', loadMarkers);
    document.getElementById('btnReset')?.addEventListener('click', () => {
        resetFilters();
        clearMarkers();
        setHint('필터를 설정하고 [적용]을 누르면 매물이 표시됩니다.');
    });

    document.addEventListener('keydown', (e) => {
        if (e.key === 'Enter') {
            if (e.target && (e.target.tagName === 'INPUT' || e.target.tagName === 'SELECT')) {
                e.preventDefault();
                loadMarkers();
            }
        }
    });

    // 최초 로딩
    if (focusId != null) {
        loadMarkers();
    } else {
        clearMarkers();
        setHint('필터를 설정하고 [적용]을 누르면 매물이 표시됩니다.');
    }
})();