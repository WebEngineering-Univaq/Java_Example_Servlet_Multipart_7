package it.univaq.f4i.iw.framework.security;

import java.util.Calendar;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public class SecurityLayer {

    //questa funzione esegue una serie di controlli di sicurezza
    //sulla sessione corrente. Se la sessione non è valida, la cancella
    //e ritorna null, altrimenti la aggiorna e la restituisce
    //this method executed a set of standard chacks on the current session.
    //If the session exists and is valid, it is returned, otherwise
    //the session is invalidated and the method returns null
    public static HttpSession checkSession(HttpServletRequest r) {
        boolean check = true;

        HttpSession s = r.getSession(false);
        //per prima cosa vediamo se la sessione è attiva
        //first, let's see is the sessione is active
        if (s == null) {
            return null;
        }

        //check sulla validità della sessione
        //second, check is the session contains valid data
        //nota: oltre a controllare se la sessione contiene un userid, 
        //dovremmo anche controllere che lo userid sia valido, probabilmente 
        //consultando il database utenti
        //note: besides checking if the session contains an userid, we should 
        //check if the userid is valid, possibly querying the user database
        if (s.getAttribute("userid") == null) {
            check = false;
            //check sull'ip del client
            //check if the client ip chaged
        } else if ((s.getAttribute("ip") == null) || !((String) s.getAttribute("ip")).equals(r.getRemoteHost())) {
            check = false;
            //check sulle date
            //check if the session is timed out
        } else {
            //inizio sessione
            //session start timestamp
            Calendar begin = (Calendar) s.getAttribute("inizio-sessione");
            //ultima azione
            //last action timestamp
            Calendar last = (Calendar) s.getAttribute("ultima-azione");
            //data/ora correnti
            //current timestamp
            Calendar now = Calendar.getInstance();
            if (begin == null) {
                check = false;
            } else {
                //secondi trascorsi dall'inizio della sessione
                //seconds from the session start
                long secondsfrombegin = (now.getTimeInMillis() - begin.getTimeInMillis()) / 1000;
                //dopo tre ore la sessione scade
                //after three hours the session is invalidated
                if (secondsfrombegin > 3 * 60 * 60) {
                    check = false;
                } else if (last != null) {
                    //secondi trascorsi dall'ultima azione
                    //seconds from the last valid action
                    long secondsfromlast = (now.getTimeInMillis() - last.getTimeInMillis()) / 1000;
                    //dopo trenta minuti dall'ultima operazione la sessione è invalidata
                    //after 30 minutes since the last action the session is invalidated                    
                    if (secondsfromlast > 30 * 60) {
                        check = false;
                    }
                }
            }
        }
        if (!check) {
            s.invalidate();
            return null;
        } else {
            //reimpostiamo la data/ora dell'ultima azione
            //if che checks are ok, update the last action timestamp
            s.setAttribute("ultima-azione", Calendar.getInstance());
            return s;
        }
    }

    public static HttpSession createSession(HttpServletRequest request, String username, int userid) {
        //se una sessione è già attiva, rimuoviamola e creiamone una nuova
        //if a session already exists, remove it and recreate a new one
        disposeSession(request);
        HttpSession s = request.getSession(true);
        s.setAttribute("username", username);
        s.setAttribute("ip", request.getRemoteHost());
        s.setAttribute("inizio-sessione", Calendar.getInstance());
        s.setAttribute("userid", userid);
        return s;
    }

    public static void disposeSession(HttpServletRequest request) {
        HttpSession s = request.getSession(false);
        if (s != null) {
            s.invalidate();
        }
    }

    public static String checkHttps(HttpServletRequest request) {
        //possiamo usare questa tecnica per controllare se la richiesta è
        //stata effettuata in https e, in caso contrario, costruire la URL
        //necessaria a ridirezionare il browser verso l'https
        //we can use this technique to check if the request was made in https 
        //and, if not, build the URL needed to redirect the browser to https
        if (request.getScheme().equals("http")) {
            String url = "https://" + request.getServerName()
                    + ":" + request.getServerPort()
                    + request.getContextPath()
                    + request.getServletPath();
            if (request.getPathInfo() != null) {
                url += request.getPathInfo();
            }
            if (request.getQueryString() != null) {
                url += "?" + request.getQueryString();
            }

            return url;
        } else {
            return null;
        }
    }

    public static String sanitizeFilename(String name) {
        return name.replaceAll("[^a-zA-Z0-9_.-]", "_");
    }        
}
