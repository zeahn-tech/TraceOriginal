import { useEffect, useState } from 'react'
import { banner, hero, logo } from '../../shared/assets'

export function Carousel(){const slides=[['Community Safety','Collaborate with local nodes to keep your neighborhood secure.',banner],['Real-time Alerts','Receive instant notifications about emergency situations nearby.',hero],['Node Network','TraceNet connects citizens and law enforcement seamlessly.',logo]];const[i,setI]=useState(0);useEffect(()=>{const id=setInterval(()=>setI(v=>(v+1)%slides.length),4000);return()=>clearInterval(id)},[]);const slide=slides[i];return <article className="carousel" style={{backgroundImage:`linear-gradient(0deg,rgba(0,0,0,.8),transparent),url(${slide[2]})`}}><span>NEW</span><div><h2>{slide[0]}</h2><p>{slide[1]}</p></div></article>}
