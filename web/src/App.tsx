import { useMemo, useState } from "react";
import {
  Activity, ArrowDownToLine, ArrowRight, BookOpen, CalendarDays, Check, ChevronDown,
  ClipboardCheck, FileText, GraduationCap, LayoutDashboard, Menu, MoreHorizontal,
  Search, Settings2, ShieldCheck, Sparkles, Users, X,
} from "lucide-react";
import { activities, schools, villages } from "./data";

type NavItem = { label: string; icon: typeof LayoutDashboard; section?: string };
const navItems: NavItem[] = [
  { label: "Ringkasan", icon: LayoutDashboard, section: "UTAMA" },
  { label: "Data guru", icon: Users },
  { label: "Sekolah binaan", icon: GraduationCap },
  { label: "Administrasi mengajar", icon: BookOpen, section: "PEMBELAJARAN" },
  { label: "Presensi siswa", icon: ClipboardCheck },
  { label: "Arsip digital", icon: FileText },
];

function Sidebar({ active, onSelect, mobileOpen, onClose }: { active: string; onSelect: (label: string) => void; mobileOpen: boolean; onClose: () => void }) {
  return (
    <>
      {mobileOpen && <button className="sidebar-scrim" aria-label="Tutup menu" onClick={onClose} />}
      <aside className={`sidebar ${mobileOpen ? "sidebar-open" : ""}`}>
        <div className="brand-row">
          <div className="brand-mark"><BookOpen size={19} strokeWidth={2.4} /></div>
          <div><strong>ADM PAI</strong><span>Kecamatan Sukajaya</span></div>
          <button className="icon-button mobile-close" aria-label="Tutup menu" onClick={onClose}><X size={18} /></button>
        </div>
        <div className="workspace-switch">
          <div className="workspace-avatar">K</div>
          <div className="workspace-copy"><span>LINGKUP KERJA</span><strong>KKG PAI Sukajaya</strong></div>
          <ChevronDown size={15} />
        </div>
        <nav className="side-nav" aria-label="Navigasi utama">
          {navItems.map((item) => {
            const Icon = item.icon;
            return <div key={item.label}>
              {item.section && <p className="nav-section">{item.section}</p>}
              <button className={`nav-item ${active === item.label ? "nav-active" : ""}`} onClick={() => { onSelect(item.label); onClose(); }} aria-current={active === item.label ? "page" : undefined}>
                <Icon size={17} strokeWidth={1.9} /><span>{item.label}</span>
                {item.label === "Administrasi mengajar" && <span className="nav-count">8</span>}
              </button>
            </div>;
          })}
        </nav>
        <div className="sidebar-bottom">
          <div className="help-card"><div className="help-icon"><Sparkles size={15} /></div><strong>Butuh bantuan?</strong><p>Panduan singkat penggunaan ADM PAI.</p><button onClick={() => window.alert("Panduan ADM PAI akan segera tersedia.")}>Lihat panduan <ArrowRight size={13} /></button></div>
          <button className="profile-row" onClick={() => window.alert("Pengaturan profil demo") }>
            <div className="profile-avatar">SN</div><span><strong>Siti Nurjanah</strong><small>Guru PAI · Admin KKG</small></span><MoreHorizontal size={18} />
          </button>
        </div>
      </aside>
    </>
  );
}

function StatCard({ title, value, note, icon: Icon, tint, trend }: { title: string; value: string; note: string; icon: typeof Users; tint: string; trend?: string }) {
  return <article className="stat-card">
    <div className="stat-top"><span>{title}</span><span className={`stat-icon ${tint}`}><Icon size={17} /></span></div>
    <div className="stat-value-row"><strong>{value}</strong>{trend && <span className="stat-trend"><Activity size={12} /> {trend}</span>}</div>
    <p>{note}</p>
  </article>;
}

function DashboardContent({ activeNav, onNavigate }: { activeNav: string; onNavigate: (label: string) => void }) {
  const [scope, setScope] = useState("KKG Kec. Sukajaya");
  const [query, setQuery] = useState("");
  const [village, setVillage] = useState("Semua desa");
  const [showAll, setShowAll] = useState(false);
  const filteredSchools = useMemo(() => schools.filter((school) => {
    const matchesQuery = `${school.name} ${school.teacher} ${school.village}`.toLowerCase().includes(query.toLowerCase());
    return matchesQuery && (village === "Semua desa" || school.village === village);
  }), [query, village]);
  const pageTitle = activeNav === "Ringkasan" ? "Ringkasan" : activeNav;

  return <main className="main-area">
    <header className="topbar">
      <div className="breadcrumb"><span>ADM PAI</span><span className="crumb-slash">/</span><strong>{pageTitle}</strong></div>
      <div className="topbar-right"><div className="sync-status"><span className="sync-dot" /> Sinkron terakhir <strong>2 menit lalu</strong></div><button className="export-button" onClick={() => window.print()}><ArrowDownToLine size={15} /> <span>Ekspor</span></button></div>
    </header>
    <div className="content-wrap">
      <div className="welcome-row">
        <div><div className="eyebrow"><span className="eyebrow-line" /> JUMAT, 27 NOVEMBER 2026</div><h1>{activeNav === "Ringkasan" ? "Selamat pagi, Bu Siti" : pageTitle}<span className="wave">.</span></h1><p className="welcome-subtitle">Berikut perkembangan administrasi guru PAI di wilayah Sukajaya.</p></div>
        <button className="date-button"><CalendarDays size={16} /><span>Semester Ganjil 2026/2027</span><ChevronDown size={14} /></button>
      </div>
      <div className="scope-tabs" role="tablist" aria-label="Lingkup dashboard">
        {["KKG Kec. Sukajaya", "Administrasi saya"].map((tab) => <button key={tab} role="tab" aria-selected={scope === tab} className={scope === tab ? "scope-active" : ""} onClick={() => setScope(tab)}>{tab === "KKG Kec. Sukajaya" ? <ShieldCheck size={15} /> : <Users size={15} />}{tab}{tab === "KKG Kec. Sukajaya" && <span className="scope-badge">33</span>}</button>)}
      </div>
      {activeNav === "Ringkasan" ? <>
        <section className="hero-card" aria-label="Ringkasan progres administrasi">
          <div className="hero-content"><div className="hero-label"><span className="hero-seal"><ShieldCheck size={15} /></span> MONITORING KKG PAI SUKAJAYA</div><h2>{scope === "KKG Kec. Sukajaya" ? "Administrasi tertata, pembelajaran bermakna." : "Semua administrasimu dalam satu tempat."}</h2><p>Pantau kelengkapan perangkat ajar dan progres sekolah binaan dalam satu tampilan.</p><div className="hero-progress"><div className="progress-meta"><span>Rata-rata kelengkapan administrasi</span><strong>82%</strong></div><div className="progress-track"><span style={{ width: "82%" }} /></div><div className="hero-foot"><span><Check size={13} /> 21 sekolah sudah mulai mengisi</span><span>Target semester ini: 100%</span></div></div></div>
          <div className="hero-art" aria-hidden="true"><div className="art-ring ring-one" /><div className="art-ring ring-two" /><div className="art-leaf leaf-one" /><div className="art-leaf leaf-two" /><div className="art-spark" /><div className="art-caption"><span>TA 2026/2027</span><strong>Ganjil</strong></div></div>
        </section>
        <section className="stats-grid" aria-label="Statistik wilayah">
          <StatCard title="Guru PAI aktif" value="33" note="di 28 sekolah binaan" icon={Users} tint="tint-green" trend="+2 bulan ini" />
          <StatCard title="Sekolah binaan" value="28" note="tersebar di 10 desa" icon={GraduationCap} tint="tint-blue" />
          <StatCard title="Administrasi lengkap" value="21" note="7 sekolah perlu tindak lanjut" icon={ClipboardCheck} tint="tint-amber" />
          <StatCard title="Dokumen tersimpan" value="146" note="dari seluruh sekolah" icon={FileText} tint="tint-violet" />
        </section>
        <section className="dashboard-grid">
          <article className="panel school-panel">
            <div className="panel-heading"><div><div className="panel-kicker">PEMANTAUAN WILAYAH</div><h3>Progres sekolah binaan</h3><p>Kelola dan pantau kelengkapan administrasi.</p></div><button className="text-link" onClick={() => setShowAll((value) => !value)}>{showAll ? "Ringkas" : "Lihat semua"}<ArrowRight size={14} /></button></div>
            <div className="table-toolbar"><label className="search-field"><Search size={15} /><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Cari sekolah atau guru..." aria-label="Cari sekolah atau guru" />{query && <button aria-label="Hapus pencarian" onClick={() => setQuery("")}><X size={14} /></button>}</label><label className="filter-select"><Settings2 size={14} /><select value={village} onChange={(event) => setVillage(event.target.value)} aria-label="Filter desa">{villages.map((item) => <option key={item}>{item}</option>)}</select></label></div>
            <div className="school-list">{filteredSchools.slice(0, showAll ? undefined : 4).map((school) => <div className="school-row" key={school.name}><div className="school-symbol">{school.name.slice(0, 1)}</div><div className="school-name"><strong>{school.name}</strong><span>{school.teacher} · {school.village}</span></div><div className="school-progress"><div className="school-progress-label"><span>Kelengkapan</span><strong>{school.completion}%</strong></div><div className="mini-track"><span style={{ width: `${school.completion}%` }} /></div></div><span className={`status-pill ${school.status === "Lengkap" ? "status-complete" : school.status === "Perlu ditinjau" ? "status-review" : "status-pending"}`}>{school.status}</span></div>)}{filteredSchools.length === 0 && <div className="empty-state">Tidak ada sekolah yang sesuai dengan pencarian.</div>}</div>
            <div className="panel-footer"><span>Menampilkan {Math.min(showAll ? filteredSchools.length : 4, filteredSchools.length)} dari 28 sekolah</span><button onClick={() => setShowAll(true)}>Buka daftar sekolah <ArrowRight size={13} /></button></div>
          </article>
          <aside className="right-column">
            <article className="panel activity-panel"><div className="panel-heading compact-heading"><div><div className="panel-kicker">AKTIVITAS TERBARU</div><h3>Catatan hari ini</h3></div><button className="small-icon-button" aria-label="Opsi aktivitas" onClick={() => window.alert("Aktivitas terbaru") }><MoreHorizontal size={18} /></button></div><div className="activity-list">{activities.map((activity, index) => <div className="activity-item" key={activity.title}><div className={`activity-icon activity-${activity.color}`}>{index === 0 ? <BookOpen size={15} /> : index === 1 ? <ClipboardCheck size={15} /> : <Sparkles size={15} />}</div><div className="activity-copy"><strong>{activity.title}</strong><span>{activity.detail}</span></div></div>)}</div><button className="activity-link" onClick={() => window.alert("Riwayat aktivitas akan segera tersedia.")}>Lihat riwayat aktivitas <ArrowRight size={14} /></button></article>
            <article className="reminder-card"><div className="reminder-icon"><CalendarDays size={17} /></div><div className="reminder-copy"><span>AGENDA BERIKUTNYA</span><strong>Rapat koordinasi KKG</strong><p>Senin, 30 Nov · 13.00 WIB</p></div><button className="reminder-more" aria-label="Detail agenda" onClick={() => window.alert("Rapat koordinasi KKG · Senin, 30 November pukul 13.00 WIB.")}><ArrowRight size={15} /></button></article>
            <article className="tip-card"><div className="tip-top"><span><Sparkles size={14} /> TIPS HARI INI</span><span className="tip-number">01 / 03</span></div><p>Lengkapi jurnal mengajar setiap selesai pembelajaran agar rekap bulanan selalu akurat.</p><button onClick={() => window.alert("Tips berikutnya akan segera tersedia.")}>Mengerti <ArrowRight size={13} /></button></article>
          </aside>
        </section>
      </> : <section className="module-empty"><div className="module-empty-icon"><BookOpen size={24} /></div><span>MODUL PREVIEW</span><h2>{activeNav}</h2><p>Tampilan ringkasan wilayah tersedia di halaman Ringkasan. Modul {activeNav.toLowerCase()} akan disambungkan pada tahap berikutnya.</p><button onClick={() => onNavigate("Ringkasan")}>Kembali ke ringkasan <ArrowRight size={14} /></button></section>}
      <footer className="page-footer"><span>ADM PAI Sukajaya <span className="footer-dot">·</span> Pratinjau web</span><span>Data contoh untuk pratinjau</span></footer>
    </div>
  </main>;
}

export default function App() {
  const [active, setActive] = useState("Ringkasan");
  const [mobileOpen, setMobileOpen] = useState(false);
  return <div className="app-shell">
    <Sidebar active={active} onSelect={setActive} mobileOpen={mobileOpen} onClose={() => setMobileOpen(false)} />
    <div className="mobile-bar"><button className="icon-button" aria-label="Buka navigasi" onClick={() => setMobileOpen(true)}><Menu size={20} /></button><div className="mobile-brand-mark"><BookOpen size={16} /></div><strong>ADM PAI</strong><span>Sukajaya</span></div>
    <DashboardContent activeNav={active} onNavigate={setActive} />
  </div>;
}
