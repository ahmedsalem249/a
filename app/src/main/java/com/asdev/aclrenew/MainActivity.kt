        return scan(content)
    }

    private fun loadAccounts() {
        val raw=prefs.getString("local_accounts",null)
        accounts.clear()
        if (!raw.isNullOrBlank()) try {
            val arr=JSONArray(raw)
            for(i in 0 until arr.length()) {
                val o=arr.getJSONObject(i)
                accounts.add(Account(
                    o.optString("id"),o.optString("name"),o.optString("email"),
                    o.optString("serviceId"),o.optString("status","ACTIVE"),
                    o.optLongOrNull("expiry"),o.optLongOrNull("lastCheck"),
                    o.optString("lastError").ifBlank{null}
                ))
            }
        } catch(_:Exception){}
        showScreen("dashboard")
    }

    private fun saveAccounts() {
        val arr=JSONArray()
        accounts.forEach { a ->
            arr.put(JSONObject().apply {
                put("id",a.id); put("name",a.name); put("email",a.email)
                put("serviceId",a.serviceId); put("status",a.status)
                a.expiry?.let{put("expiry",it)}
                a.lastCheck?.let{put("lastCheck",it)}
                a.lastError?.let{put("lastError",it)}
            })
        }
        prefs.edit().putString("local_accounts",arr.toString()).apply()
    }

    private fun stat(parent: LinearLayout, label: String): TextView {
        val box=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL; gravity=Gravity.CENTER_VERTICAL
            setPadding(13.dp(),12.dp(),13.dp(),12.dp()); background=rounded(CARD,16.dp())
        }
        parent.addView(box,LinearLayout.LayoutParams(0,75.dp(),1f).apply{setMargins(0,0,7.dp(),0)})
        val n=TextView(this).apply{text="0";textSize=22f;setTextColor(WHITE);typeface=Typeface.DEFAULT_BOLD}
        box.addView(n)
        box.addView(TextView(this).apply{text=label;textSize=9f;setTextColor(MUTED)})
        return n
    }

    private fun info(t:String,v:String)=LinearLayout(this).apply{
        orientation=LinearLayout.VERTICAL
        addView(TextView(this@MainActivity).apply{text=t;textSize=8f;setTextColor(MUTED)})
        addView(TextView(this@MainActivity).apply{text=v;textSize=11f;setTextColor(WHITE);typeface=Typeface.DEFAULT_BOLD;setPadding(0,3.dp(),0,0)})
    }

    private fun section(s:String)=TextView(this).apply{
        text=s;textSize=11f;setTextColor(BLUE);typeface=Typeface.DEFAULT_BOLD;setPadding(0,20.dp(),0,8.dp())
    }

    private fun toggleRow(title:String,desc:String,value:Boolean,onChange:(Boolean)->Unit)=LinearLayout(this).apply{
        orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(14.dp(),10.dp(),8.dp(),10.dp());background=rounded(CARD,15.dp())
        val texts=LinearLayout(this@MainActivity).apply{orientation=LinearLayout.VERTICAL;layoutParams=LinearLayout.LayoutParams(0,WRAP,1f)}
        texts.addView(TextView(this@MainActivity).apply{text=title;textSize=14f;setTextColor(WHITE);typeface=Typeface.DEFAULT_BOLD})
        texts.addView(TextView(this@MainActivity).apply{text=desc;textSize=10f;setTextColor(MUTED)})
        addView(texts);addView(Switch(this@MainActivity).apply{isChecked=value;setOnCheckedChangeListener{_,v->onChange(v)}})
        layoutParams=LinearLayout.LayoutParams(MATCH,WRAP).apply{setMargins(0,0,0,8.dp())}
    }

    private fun input(h:String,v:String)=EditText(this).apply{
        hint=h;setText(v);setTextColor(WHITE);setHintTextColor(MUTED);setSingleLine(true)
        background=rounded(CARD2,12.dp());setPadding(12.dp(),0,12.dp(),0)
        layoutParams=LinearLayout.LayoutParams(MATCH,48.dp()).apply{setMargins(0,0,0,10.dp())}
    }

    private fun emptyState()=LinearLayout(this).apply{
        orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER;setPadding(20.dp(),60.dp(),20.dp(),60.dp());background=rounded(CARD,20.dp())
        addView(TextView(this@MainActivity).apply{text="☁";textSize=42f;gravity=Gravity.CENTER;setTextColor(BLUE)})
        addView(TextView(this@MainActivity).apply{text="No accounts yet";textSize=18f;gravity=Gravity.CENTER;setTextColor(WHITE);typeface=Typeface.DEFAULT_BOLD})
        addView(TextView(this@MainActivity).apply{text="Add up to five ACLClouds accounts.";textSize=12f;gravity=Gravity.CENTER;setTextColor(MUTED);setPadding(0,6.dp(),0,0)})
    }

    private fun button(text:String,bg:Int,fg:Int)=TextView(this).apply{
        this.text=text;gravity=Gravity.CENTER;textSize=11f;setTextColor(fg);typeface=Typeface.DEFAULT_BOLD
        background=rounded(bg,13.dp());isClickable=true;isFocusable=true
    }

    private fun expiryText(e:Long?):String{
        if(e==null)return "—"
        val d=e-System.currentTimeMillis()
        if(d<=0)return "EXPIRED"
        val s=d/1000;return "${s/86400}d ${(s%86400)/3600}h ${(s%3600)/60}m ${s%60}s"
    }

    private fun countdownColor(e:Long?):Int{
        if(e==null)return MUTED
        val d=e-System.currentTimeMillis()
        return when{d<=0->RED;d<=86400000L->RED;d<=172800000L->ORANGE;else->GREEN}
    }

    private fun formatDate(t:Long?):String=if(t==null)"—" else try{SimpleDateFormat("dd MMM, HH:mm",Locale.getDefault()).format(Date(t))}catch(_:Exception){"—"}
    private fun accountNumber(a:Account):String=a.id.filter{it.isDigit()}.toIntOrNull()?.toString()?:"01"
    private fun rounded(c:Int,r:Int)=GradientDrawable().apply{setColor(c);cornerRadius=r.toFloat()}
    private fun toast(s:String)=Toast.makeText(this,s,Toast.LENGTH_LONG).show()
    private fun Int.dp()=(this*resources.displayMetrics.density).toInt()
    private val MATCH=ViewGroup.LayoutParams.MATCH_PARENT
    private val WRAP=ViewGroup.LayoutParams.WRAP_CONTENT
}

private fun JSONObject.optLongOrNull(name:String):Long?{
    if(!has(name)||isNull(name))return null
    return when(val v=opt(name)){is Number->v.toLong();else->v.toString().toLongOrNull()}
}
