export default {
  title: 'Scoula', // 메인 타이틀
  subtitle: 'KB Fullstack 학습(Vue+Spring)', // 서브 타이틀
  menus: [],
  accountMenus: {
    // 인증 관련 메뉴 정보
    login: {
      url: '/login',
      title: '로그인',
      icon: 'fa-solid fa-right-to-bracket',
    },

    join: {
      url: '/signup',
      title: '회원가입',
      icon: 'fa-solid fa-user-plus',
    },
  },
};
