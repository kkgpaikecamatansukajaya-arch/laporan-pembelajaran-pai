export type School = {
  name: string;
  village: string;
  teacher: string;
  completion: number;
  status: "Lengkap" | "Perlu ditinjau" | "Belum lengkap";
};

export const schools: School[] = [
  { name: "SDN Sukajaya 01", village: "Sukajaya", teacher: "Siti Nurjanah, S.Pd.I", completion: 96, status: "Lengkap" },
  { name: "SDN Kiarapandak 02", village: "Kiarapandak", teacher: "Ahmad Fauzi, S.Pd.I", completion: 88, status: "Perlu ditinjau" },
  { name: "SDN Pasir Madang 01", village: "Pasir Madang", teacher: "Rina Marlina, S.Pd.I", completion: 82, status: "Perlu ditinjau" },
  { name: "SDN Cileuksa 03", village: "Cileuksa", teacher: "Dedi Suhendi, S.Pd.I", completion: 74, status: "Belum lengkap" },
  { name: "SDN Harkatjaya 01", village: "Harkatjaya", teacher: "Nia Kurniasih, S.Pd.I", completion: 69, status: "Belum lengkap" },
];

export const villages = ["Semua desa", "Sukajaya", "Kiarapandak", "Pasir Madang", "Cileuksa", "Harkatjaya"];

export const activities = [
  { title: "Jurnal mengajar kelas 5", detail: "SDN Sukajaya 01 · 08.15", kind: "Jurnal", color: "green" },
  { title: "Rekap presensi mingguan", detail: "SDN Kiarapandak 02 · 09.40", kind: "Presensi", color: "blue" },
  { title: "Setoran hafalan surat pendek", detail: "SDN Pasir Madang 01 · 10.20", kind: "Hafalan", color: "amber" },
];
