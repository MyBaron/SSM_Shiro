# SSM_Shiro
Shiro与SSM结合，实现登录和授权功能

## 本人博客
https://www.jianshu.com/u/4a02af32281e

## 开篇
1. 本项目已经上传)[github](https://github.com/MyBaron/SSM_Shiro)，建议对照代码理解
2. 本篇主要讲Shiro框架与SSM框架结合，实现登录和授权功能
3.  利用spring 的aop切面思想，很简单得融合Shiro权限框架
4. [代码](https://github.com/MyBaron/SSM_Shiro)

> 需要明白两个点：
> 1. 通过Subject.login() 登录成功后，用户信息就会保存在安全管理器上，也就是 SecurityManager。就可以在程序任何地方获取到该用户对象。
> 2. 在重写拦截器两个方法是重点，在登录的时候就需要把授权信息也存到安全管理器上，所以登录成功后，所有判断权限都不需要在业务逻辑上做判断，shiro框架已经帮你拦截并判断好。

## 1. jar包
shiro有很多种类型的包,用途有web的，非web的等，*-all 代表所有都在里面
``shiro-all.jar``

## 2.配置
其实shiro权限控制就是通过拦截器来进行判断用户权限的，因此shiro拦截器的配置跟springMVC的拦截器配置是类似的。

###  第一步
尽然是通过aop来使用shiro，那就需要在web.xml里添加一个shiro的拦截器。
> ps:这个shiro拦截器是如何加载的呢？ 因为这个项目shiro与spring整合了，所有运行项目的时候，spring监听器会去寻找并加载shiro拦截器
 ```
 <!-- Spring监听器 -->
  <listener>
    <listener-class>org.springframework.web.context.ContextLoaderListener</listener-class>
  </listener>
 ```


shiro拦截器配置如下：

``` xml
<!-- 配置由Spring提供的过滤器，用于整合shiro框架 -->
  <!-- 在项目启动的过程中，当前过滤器会从Spring工厂中提取同名对象 -->
  <filter>
    <filter-name>shiroFilter</filter-name>
    <filter-class>
      org.springframework.web.filter.DelegatingFilterProxy
    </filter-class>
  </filter>
  <filter-mapping>
    <filter-name>shiroFilter</filter-name>
    <url-pattern>/*</url-pattern>
  </filter-mapping>
```

### 第二步
类似springmvc一样，需要写一个配置文件来配置拦截器。
在shiro配置文件中，有两种类型。
	- ini配置文件 （很多博客教程上都是使用这种配置方式。）
	- xml配置文件 （这次项目使用这种配置方式）

首先创建shiro-context.xml
> ps: 很简单，就相当于spring的配置文件一样，因为shiro是跟spring很好结合的。
> 下面会慢慢解释这个配置文件

``` xml
<bean id="shiroFilter" class="org.apache.shiro.spring.web.ShiroFilterFactoryBean">
		<property name="securityManager" ref="securityManager" /> <!--加载管理器-->
		<property name="loginUrl" value="/user/login" />    <!--没有登录的时候，跳转到这个页面-->
		<property name="unauthorizedUrl" value="/user/nopermission" /> <!--当没有权限的时候，跳转到这个url-->

		<property name="filterChainDefinitions">
			<value>
				/user/login = anon <!--可以不需要登录-->
				/user/readName = authc, perms[/readName]  <!-- perms 表示需要该权限才能访问的页面 -->
				/user/readData = authc, perms[/readData]
				/user/* = authc <!-- authc 表示需要认证才能访问的页面 -->
			</value>
		</property>
	</bean>

	<!-- 自定义Realm -->
	<bean id="myShiroRealm" class="com.Shiro.MyShiroReaml">
		<!-- businessManager 用来实现用户名密码的查询 -->
		<property name="shiroService" ref="accountService" />
	</bean>

	<bean id="securityManager" class="org.apache.shiro.web.mgt.DefaultWebSecurityManager">
		<!-- 注入realm -->
		<property name="realm" ref="myShiroRealm"/>
	</bean>

	<!--声明一个Service 注入到自定义Realm-->
	<bean id="accountService" class="com.Service.Impl.ShiroServiceImpl"/>
	<!-- <bean id="shiroCacheManager" class="org.apache.shiro.cache.ehcache.EhCacheManager">
		<property name="cacheManager" ref="cacheManager" /> </bean> -->
</beans>
```

1. 拦截器需要加载一个安全管理器，SecurityManager 是整个shiro框架的核心
``` xml
	<property name="securityManager" ref="securityManager" /> <!--加载管理器-->
```

2. 拦截url,这里就是配置拦截url。anon.authc.这些名词其实就是shiro已经写好的拦截器，只需要调用可以了。在如果权限不够，则会跳转到指定的url
```
  <property name="filterChainDefinitions">
			<value>
				/user/login = anon <!--可以不需要登录-->
				/user/readName = authc, perms[/readName]  <!-- perms 表示需要该权限才能访问的页面 -->
				/user/readData = authc, perms[/readData]
				/user/* = authc <!-- authc 表示需要认证才能访问的页面 -->
			</value>
		</property>
```
过滤器列表：


![208783.jpg](http://upload-images.jianshu.io/upload_images/6212571-df692703a5785cd7.jpg?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)



### 第三步 拦截后，对用户进行验证
当你拦截器设置好了，可以成功拦截用户的操作，然后我们需要对用户进行权限验证。所以我们需要继承shiro的AuthorizingRealm拦截器，重写两个方法。
	- 重写doGetAuthenticationInfo方法是：登录验证，当需要登录的时候，就会调用该方法进行验证。
	- 重写doGetAuthorizationInfo方法：这个是授权验证，与上面的过滤器相结合。

思路如下：
   - 登录验证： 根据账号从数据库获取账号密码进行比较，如果一致则登录成功，就会保存到，否则登录失败
   - 授权验证：在登录成功后，根据用户id获取到该用户的权限，并把权限保存在安全管理器之中，当用户访问的时候，会从管理器中判断该用户是否有权限去访问该url。
代码如下：
``` java
public class MyShiroReaml extends AuthorizingRealm {
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection pc) {

        /**
         *
         * 流程
         * 1.根据用户user->2.获取角色id->3.根据角色id获取权限permission
         */
        //方法一：获得user对象
        User user=(User)pc.getPrimaryPrincipal();
        SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();
        //获取permission
        if(user!=null) {
            List<Permission> permissionsByUser = shiroService.getPermissionsByUser(user);
            if (permissionsByUser.size()!=0) {
                for (Permission p: permissionsByUser) {

                    info.addStringPermission(p.getUrl());
                }
                return info;
            }
        }

        //方法二： 从subject管理器里获取user
//      Subject subject = SecurityUtils.getSubject();
//      User _user = (User) subject.getPrincipal();
//      System.out.println("subject"+_user.getUsername());




        return null;
    }

    // 认证方法
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authenticationToken) throws AuthenticationException {
        System.out.println("进来验证了");
        //验证账号密码
        UsernamePasswordToken token = (UsernamePasswordToken) authenticationToken;
        System.out.println("1:"+token.getUsername());
        User user = shiroService.getUserByUserName(token.getUsername());
        System.out.println("2");
        if(user==null){
            return null;
        }
        //最后的比对需要交给安全管理器
        //三个参数进行初步的简单认证信息对象的包装
        AuthenticationInfo info = new SimpleAuthenticationInfo(user, user.getPassword(), this.getClass().getSimpleName());

        return info;
    }


    private ShiroService shiroService;

    public ShiroService getShiroService() {
        return shiroService;
    }

    public void setShiroService(ShiroService shiroService) {
        this.shiroService = shiroService;
    }
}

```

### 第四步
shrio的基本配置已经完成了，接下来是基础代码块了， 也许还有很多疑问，没关系。下载代码下来看看，就明白了

#### Controller层
1.登录代码块
```
 @RequestMapping(value = "/login")
    public String Login(String username, String password, HttpSession session, Model model){
        if(username==null){
            model.addAttribute("message", "账号不为空");
            return "login";
        }
        //主体,当前状态为没有认证的状态“未认证”
        Subject subject = SecurityUtils.getSubject();
        // 登录后存放进shiro token
        UsernamePasswordToken token=new UsernamePasswordToken(username,password);
        User user;
        //登录方法（认证是否通过）
        //使用subject调用securityManager,安全管理器调用Realm
        try {
            //利用异常操作
            //需要开始调用到Realm中
            System.out.println("========================================");
            System.out.println("1、进入认证方法");
            subject.login(token);
            user = (User)subject.getPrincipal();
            session.setAttribute("user",subject);
            model.addAttribute("message", "登录完成");
            System.out.println("登录完成");
        } catch (UnknownAccountException e) {
            model.addAttribute("message", "账号密码不正确");
            return "index";
        }
        return "test";
    }

```

2. 权限代码块
```
shiro已经帮我们验证了，所以我们只需要写基本业务逻辑就可以，不需要再写权限验证代码了
 @RequestMapping("/readName")
    public String readName(HttpSession session){

        return "name";
    }

    @RequestMapping("/readData")
    public String readData(){

        return "data";
    }


    @RequestMapping("/nopermission")
    public String noPermission(){
        return "error";
    }
```

#### Service 层
``` java
public class ShiroServiceImpl implements ShiroService {

    @Autowired
    private ShiroDao shiroDao;

    public User getUserByUserName(String username) {
        //根据账号获取账号密码
        User userByUserName = shiroDao.getUserByUserName(username);
        return userByUserName;
    }

    public List<Permission> getPermissionsByUser(User user) {
        //获取到用户角色userRole
        List<Integer> roleId = shiroDao.getUserRoleByUserId(user.getId());
        List<Permission> perArrary = new ArrayList<>();

        if (roleId!=null&&roleId.size()!=0) {
            //根据roleid获取peimission
            for (Integer i : roleId) {
                perArrary.addAll(shiroDao.getPermissionsByRoleId(i));
            }
        }

        System.out.println(perArrary);
        return perArrary;
    }


}

```

#### POJO层
``` java
public class Permission {
    private int id;
    private String token;
    /**资源url**/
    private String url;
    /**权限说明**/
    private String description;
    /**所属角色编号**/
    private int roleId;
    }

public class Role {
    private int id;
    /**角色**/
    private String role;
    /**说明**/
    private String description;
    }

public class User {
    private int id;
    private String account;
    private String password;
}
```

### 第5步 数据库表的建立
数据表已经附加到代码上传到github
数据库中添加一条用户、角色、以及权限数据，并且在关联表中添加一条关联数据
- Permission 权限表

![Permission.png](http://upload-images.jianshu.io/upload_images/6212571-4d5eee63e0e6c47d.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

- 角色管理表

![角色管理表.png](http://upload-images.jianshu.io/upload_images/6212571-fc77095b505ae03f.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

 - 用户表

![用户表.png](http://upload-images.jianshu.io/upload_images/6212571-9e8100af808379a4.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

  - 用户角色对照表

![用户角色对照表.png](http://upload-images.jianshu.io/upload_images/6212571-238ae1577ac7c900.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)



## 总结
1. Shiro框架 与spring结合，对原有的代码块改变很少。完美得实现了AOP思想
2. Shiro的功能很强大，该项目实现了登录已经授权的基本功能，接下来还会有单点登录，缓存机制，密码加密等功能的演示。
3. 代码已放到github上，可以clone下来细看。
4. [代码](https://github.com/MyBaron/SSM_Shiro)
