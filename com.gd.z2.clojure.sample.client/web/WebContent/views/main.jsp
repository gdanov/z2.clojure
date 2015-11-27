<%@ page import="com.zfabrik.components.IComponentsLookup" %>
<%@ page import="com.zfabrik.components.java.IJavaComponent" %>
<%@ page import="com.gd.IClojureComponent" %>
<%@ page import="java.util.concurrent.Callable" %>
<%@ page import="java.lang.reflect.Method" %>
<%--
for whatever reason this does not work. Says Recort is a package...
<%@ page import="dep.Recort" %>--%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@page contentType="text/html; charset=utf-8" %>
<html>
<head>
    <style>
        table#thingies {
            width: 100%;
        }

        table#thingies tr:nth-child(even) {
            background-color: #eee;
        }

        table#thingies tr:nth-child(odd) {
            background-color: #fff;
        }

        table#thingies td.action {
            text-align: center;
        }
    </style>
</head>

<body onload="document.forms['input'].name.focus();">

<%! Object res; %>

<% IClojureComponent comp = IComponentsLookup.INSTANCE.lookup("client/clj", IClojureComponent.class);
    res = ((Callable) comp.invoke("dep", "xchange-objects")).call();
%>

Do something in clojure : <%= res.getClass() %>
<br/>
<% for (Class i : res.getClass().getInterfaces()){%>
<br/><%= i %>
<% } %>
<br/>

<% for (Method i : res.getClass().getDeclaredMethods()){%>
<%--<br/><%= i %>--%>
<% } %>

<%= res.getClass().getClassLoader().getParent().getParent() %>

<%--
understand/explain why this does not work
<%= Class.forName("clojure.core") %>--%>

<!--
dep> (.getClassLoader Recort)
#object[clojure.lang.DynamicClassLoader 0x7c8cead "clojure.lang.DynamicClassLoader@7c8cead"]
dep> (-> Recort .getClassLoader)
#object[clojure.lang.DynamicClassLoader 0x7c8cead "clojure.lang.DynamicClassLoader@7c8cead"]
dep> (-> Recort .getClassLoader .getParent)
#object[clojure.lang.DynamicClassLoader 0x5a044704 "clojure.lang.DynamicClassLoader@5a044704"]
dep> (-> Recort .getClassLoader .getParent .getParent)
#object[clojure.lang.DynamicClassLoader 0x794ec9cc "clojure.lang.DynamicClassLoader@794ec9cc"]
dep> (-> Recort .getClassLoader .getParent .getParent .getParent)
#object[clojure.lang.DynamicClassLoader 0x6eb38908 "clojure.lang.DynamicClassLoader@6eb38908"]
dep> (-> Recort .getClassLoader .getParent .getParent .getParent .getParent)
#object[clojure.lang.DynamicClassLoader 0x4ce9a4ff "clojure.lang.DynamicClassLoader@4ce9a4ff"]
dep> (-> Recort .getClassLoader .getParent .getParent .getParent .getParent .getParent)
#object[clojure.lang.DynamicClassLoader 0x344d532e "clojure.lang.DynamicClassLoader@344d532e"]
dep> (-> Recort .getClassLoader .getParent .getParent .getParent .getParent .getParent .getParent)
#object[clojure.lang.DynamicClassLoader 0x477c7b19 "clojure.lang.DynamicClassLoader@477c7b19"]
dep> (-> Recort .getClassLoader .getParent .getParent .getParent .getParent .getParent .getParent .getParent)
#object[com.zfabrik.impl.components.java.ComponentClassLoader 0x146685d "com.zfabrik.impl.components.java.ComponentClassLoader@146685d:client/clj clojure classloader [ok]"]
-->

<br/>


<h1>Add</h1>

<form action="?add" method="post" name="input">
    <input type="text" name="name">
    <input type="submit" value="add">
</form>

</body>
</html>